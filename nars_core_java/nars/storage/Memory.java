/* 
 * The MIT License
 *
 * Copyright 2019 The OpenNARS authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package nars.storage;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import nars.entity.BudgetValue;
import nars.entity.Concept;
import nars.entity.Item;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.entity.TruthValue;
import nars.gui3d.Frame3D;
import nars.gui3d.Show3D;
import nars.inference.BudgetFunctions;
import nars.io.IInferenceRecorder;
import nars.language.Term;
import nars.main_nogui.Parameters;
import nars.main_nogui.ReasonerBatch;

import static nars.gui3d.Show3D.*;

/**
 * The memory of the system.
 */
public class Memory {

    /**
     * Backward pointer to the reasoner
     */
    private final ReasonerBatch reasoner;



    /* ---------- Long-term storage for multiple cycles 长期任务, 跨越多个循环的任务 ---------- */
    /**
     * Concept bag. Containing all Concepts of the system
     * 记忆中所有的概念集合. 如果 newTasks 中没有任务, 会在这里激发新任务.
     */
    private final ConceptBag concepts;
    /**
     * New tasks with novel composed terms, for delayed and selective processing
     * 新奇的经验缓冲区(由新的概念组合所标识的任务), 值得长时间研究的任务(当然也有淘汰,因为有 Bag 上限),
     * 这里通常是 nars 自己探索得来的经验值较高的任务, 不是研究员直接分配的任务.
     * 如果 newTasks 里没有任务, 意味着上一轮的没有任务遗留或已经处理完了, 则会自动从 novelTasks 中挑选任务.
     * (主要是概念生成开销大,实时跑的时候,会生成大量概念, 但很多概念不重要, 所以放到 bag 里面, 排队和剔除.)
     */
    private final NovelTaskBag novelTasks; // 新概念,新任务,往往是由小任务组合而成,不紧急,但值得探索. (如: 学习, 探索)




    /* ---------- Short-term workspace for a single cycle 短期触发的任务 ---------- */
    /**
     * List of new tasks accumulated in one cycle, to be processed in the next cycle
     * 本次循环积累的短时任务, 会在下个循环中处理 (也可以反过来说: 上次循环积累的短时任务, 需要在本次循环中处理).
     */
    private final LinkedList<Task> newTasks; // 旧任务/输入任务/紧急任务. (如: 骑车,拿筷子,咀嚼,翻书,躲避危险,接受命令/任务)




    /** Inference record text to be written into a log file */
    private IInferenceRecorder recorder;

    /** 下面是三个原子整数, 本质上就是整数, 用于在 UI 线程和 推理线程 之间共享参数 */
    private final AtomicInteger beliefForgettingRate = new AtomicInteger(Parameters.TERM_LINK_FORGETTING_CYCLE); // 知识(信仰)遗忘率
    private final AtomicInteger taskForgettingRate = new AtomicInteger(Parameters.TASK_LINK_FORGETTING_CYCLE);   // 任务遗忘率
    private final AtomicInteger conceptForgettingRate = new AtomicInteger(Parameters.CONCEPT_FORGETTING_CYCLE);  // 概念遗忘率
    /**
     * List of Strings or Tasks to be sent to the output channels
     */
    private final ArrayList<String> exportStrings;
    /**
     * The selected Term
     */
    public Term currentTerm;
    /**
     * The selected Concept
     */
    public Concept currentConcept;
    /**
     * The selected TaskLink
     */
    public TaskLink currentTaskLink;
    /**
     * The selected Task
     */
    public Task currentTask;
    /**
     * The selected TermLink
     */
    public TermLink currentBeliefLink;
    /**
     * The selected belief
     */
    public Sentence currentBelief;
    /**
     * The new Stamp
     */
    public Stamp newStamp;
    /**
     * The substitution that unify the common term in the Task and the Belief
     * TODO unused
     */
    protected HashMap<Term, Term> substitute;

    public static Random randomNumber = new Random(1);
    
    /* ---------- Constructor ---------- */
    /**
     * Create a new memory <p> Called in Reasoner.reset only
     *
     * @param reasoner
     */
    public Memory(ReasonerBatch reasoner) {
        this.reasoner = reasoner;
        recorder = new NullInferenceRecorder();
        concepts = new ConceptBag(this);
        novelTasks = new NovelTaskBag(this);
        newTasks = new LinkedList<>();
        exportStrings = new ArrayList<>();
    }

    public void init() {
        concepts.init();
        novelTasks.init();
        newTasks.clear();
        exportStrings.clear();
        reasoner.initTimer();
        randomNumber = new Random(1);
        recorder.append("\n-----RESET-----\n");
    }

    /* ---------- access utilities ---------- */
    public ArrayList<String> getExportStrings() {
        return exportStrings;
    }

    public IInferenceRecorder getRecorder() {
        return recorder;
    }

    public void setRecorder(IInferenceRecorder recorder) {
        this.recorder = recorder;
    }

    public long getTime() {
        return reasoner.getTime();
    }

//    public MainWindow getMainWindow() {
//        return reasoner.getMainWindow();
//    }
    /**
     * Actually means that there are no new Tasks
     */
    public boolean noResult() {
        return newTasks.isEmpty();
    }

    /* ---------- conversion utilities ---------- */
    /**
     * Get an existing Concept for a given name <p> called from Term and
     * ConceptWindow.
     *
     * @param name the name of a concept
     * @return a Concept or null
     */
    public Concept nameToConcept(String name) {
        return concepts.get(name);
    }

    /**
     * Get a Term for a given name of a Concept or Operator <p> called in
     * StringParser and the make methods of compound terms.
     *
     * @param name the name of a concept or operator
     * @return a Term or null (if no Concept/Operator has this name)
     */
    public Term nameToListedTerm(String name) {
        Concept concept = concepts.get(name);
        if (concept != null) {
            return concept.getTerm();
        }
        return null;
    }

    /**
     * Get an existing Concept for a given Term.
     *
     * @param term The Term naming a concept
     * @return a Concept or null
     */
    public Concept termToConcept(Term term) {
        return nameToConcept(term.getName());
    }

    /**
     * Get the Concept associated to a Term, or create it.
     *
     * @param term indicating the concept
     * @return an existing Concept, or a new one, or null ( TODO bad smell )
     */
    public Concept getConcept(Term term) {
        if (!term.isConstant()) {
            return null;
        }
        String n = term.getName();
        Concept concept = concepts.get(n);
        if (concept == null) {
            concept = new Concept(term, this); // the only place to make a new Concept
            boolean created = concepts.putIn(concept);
            if (created) {
                Show3D.inst().append(INSERT_CONCEPT, concept);
            }else{
                return null;
            }
        }
        return concept;
    }

    /**
     * Get the current activation level of a concept.
     *
     * @param t The Term naming a concept
     * @return the priority value of the concept
     */
    public float getConceptActivation(Term t) {
        Concept c = termToConcept(t);
        return (c == null) ? 0f : c.getPriority();
    }

    /* ---------- adjustment functions ---------- */
    /**
     * Adjust the activation level of a Concept <p> called in
     * Concept.insertTaskLink only
     *
     * @param c the concept to be adjusted
     * @param b the new BudgetValue
     */
    public void activateConcept(Concept c, BudgetValue b) {
        concepts.pickOut(c.getKey());
        BudgetFunctions.activate(c, b);
        concepts.putBack(c);
    }

    /* ---------- new task entries ---------- */

    /* There are several types of new tasks, all added into the
     newTasks list, to be processed in the next workCycle.
     Some of them are reported and/or logged. */
    /**
     * Input task processing. Invoked by the outside or inside environment.
     * Outside: StringParser (input); Inside: Operator (feedback). Input tasks
     * with low priority are ignored, and the others are put into task buffer.
     *
     * @param task The input task
     */
    public void inputTask(Task task) {
        if (task.getBudget().aboveThreshold()) {
            recorder.append("!!! Perceived: " + task + "\n");
            report(task.getSentence(), true);    // report input
            newTasks.add(task);       // wait to be processed in the next workCycle // 等到下一个循环再做处理
            // 上面这里或许可以改成 novelTasks.putIn(task) ? 或者合并 novelTasks 和 newTasks ?
        } else {
            recorder.append("!!! Neglected: " + task + "\n");
            Show3D.inst().remove(task);
        }
    }

    /**
     * Activated task called in MatchingRules.trySolution and
     * Concept.processGoal
     *
     * @param budget The budget value of the new Task
     * @param sentence The content of the new Task
     * @param candidateBelief The belief to be used in future inference, for
     * forward/backward correspondence
     */
    public void activatedTask(BudgetValue budget, Sentence sentence, Sentence candidateBelief) {
        Task task = new Task(sentence, budget, currentTask, sentence, candidateBelief);
        recorder.append("!!! Activated: " + task.toString() + "\n");
        if (sentence.isQuestion()) {
            float s = task.getBudget().summary();
//            float minSilent = reasoner.getMainWindow().silentW.value() / 100.0f;
            float minSilent = reasoner.getSilenceValue().get() / 100.0f;
            if (s > minSilent) {  // only report significant derived Tasks
                report(task.getSentence(), false);
            }
        }
        newTasks.add(task);
    }

    /**
     * Derived task comes from the inference rules.
     *
     * @param task the derived task
     */
    private void derivedTask(Task task) {
        if (task.getBudget().aboveThreshold()) {
            recorder.append("!!! Derived: " + task + "\n");
            Show3D.inst().append(DERIVED,task);
            float budget = task.getBudget().summary();
//            float minSilent = reasoner.getMainWindow().silentW.value() / 100.0f;
            float minSilent = reasoner.getSilenceValue().get() / 100.0f;
            if (budget > minSilent) {  // only report significant derived Tasks
                report(task.getSentence(), false);
                Show3D.inst().move(task);
            }
            newTasks.add(task);
        } else {
            recorder.append("!!! Ignored: " + task + "\n");
            Show3D.inst().remove(task);
        }
    }

    /* --------------- new task building --------------- */
    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     */
    public void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        if (newContent != null) {
            Sentence newSentence = new Sentence(newContent, currentTask.getSentence().getPunctuation(), newTruth, newStamp);
            Task newTask = new Task(newSentence, newBudget, currentTask, currentBelief);
            derivedTask(newTask);
        }
    }

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     * @param revisible Whether the sentence is revisible
     */
    public void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget, boolean revisible) {
        if (newContent != null) {
            Sentence taskSentence = currentTask.getSentence();
            Sentence newSentence = new Sentence(newContent, taskSentence.getPunctuation(), newTruth, newStamp, revisible);
            Task newTask = new Task(newSentence, newBudget, currentTask, currentBelief);
            derivedTask(newTask);
        }
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     */
    public void singlePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        singlePremiseTask(newContent, currentTask.getSentence().getPunctuation(), newTruth, newBudget);
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param punctuation The punctuation of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     */
    public void singlePremiseTask(Term newContent, char punctuation, TruthValue newTruth, BudgetValue newBudget) {
        Task parentTask = currentTask.getParentTask();
        if (parentTask != null && newContent.equals(parentTask.getContent())) { // circular structural inference
            return;
        }
        Sentence taskSentence = currentTask.getSentence();
        if (taskSentence.isJudgment() || currentBelief == null) {
            newStamp = new Stamp(taskSentence.getStamp(), getTime());
        } else {    // to answer a question with negation in NAL-5 --- move to activated task?
            newStamp = new Stamp(currentBelief.getStamp(), getTime());
        }
        Sentence newSentence = new Sentence(newContent, punctuation, newTruth, newStamp, taskSentence.getRevisible());
        Task newTask = new Task(newSentence, newBudget, currentTask, null);
        derivedTask(newTask);
    }

    /* ---------- system working workCycle ---------- */
    /**
     * An atomic working cycle of the system: process new Tasks, then fire a
     * concept <p> Called from Reasoner.tick only
     * 单次思考循环, 完成累积任务, 并激发新任务.
     * @param clock The current time to be displayed
     */
    public void workCycle(long clock) {
        recorder.append(" --- " + clock + " ---\n");
        processNewTask();       // 处理上一个循环遗留下来的紧急/短时任务.                        (例: 骑车)
        if (noResult()) {
            processNovelTask(); // 如果下一轮空闲, 则启动分支高经验值长时任务做一做吧. 总不能闲着. (例: 学习)
        }
        if (noResult()) {
            processConcept();   // 如果下一轮空闲, 随机地激发某个概念做推理.                      (例: 白日梦)
        }
        novelTasks.refresh();   // 让侦听器更新 UI.
    }

    /**
     * Process the newTasks accumulated in the previous workCycle, accept input
     * ones and those that corresponding to existing concepts, plus one from the
     * buffer.
     * 处理上一个循环中在 newTasks (紧急任务) 中累积的任务, 如果是用户输入的概念或者旧概念, 则立即处理,
     * 如果存在新概念, 则放到长期缓冲区 novelTasks (不紧急/但重要), 等待下一个循环, 在 processNovelTask 中处理.
     * 所有过程都可能会向 newTasks 中加入新任务(输入概念或已知概念).  novelTasks 只针对新概念.
     */
    private void processNewTask() {
        Task task;

        // don't include new tasks produced in the current workCycle
        // 不处理本次循环中产生的任务, 而是处理上一轮循环中产生的新任务.
        int counter = newTasks.size();

        while (counter-- > 0) {//只要任务量大于0 , 就处理, 然后打钩.
            task = newTasks.removeFirst();
            if (task.isInput() || (termToConcept(task.getContent()) != null)) {
                // new input or existing concept
                // 如果是输入任务, 或已有对应的旧概念, 则立即处理
                immediateProcess(task);
            } else {
                // 如果不是用户输入的新概念, 但确实是之前没有的概念, 那么:
                Sentence s = task.getSentence();
                if (s.isJudgment()) {
                    double d = s.getTruth().getExpectation();
                    if (d > Parameters.DEFAULT_CREATION_EXPECTATION) {  // 经验够格才会新建新任务和概念
                        novelTasks.putIn(task);    // new concept formation 新任务/概念被加入 Bag, 等待稍后的处理
                    } else {
                        recorder.append("!!! Neglected: " + task + "\n");// 经验值不够则遗忘此任务
                        Show3D.inst().remove(task);
                    }
                }
            }
        }
    }

    /**
     * Select a novel task to process.
     */
    private void processNovelTask() {
        Task task = novelTasks.takeOut();       // select a task from novelTasks
        if (task != null) {
            immediateProcess(task);
        }
    }

    /**
     * Select a concept to fire.
     */
    public void processConcept() {
        currentConcept = concepts.takeOut();
        if (currentConcept != null) {
            currentTerm = currentConcept.getTerm();
            recorder.append(" * Selected Concept: " + currentTerm + "\n");
            concepts.putBack(currentConcept);   // current Concept remains in the bag all the time
            currentConcept.fire();              // a working workCycle
        }
    }

    /* ---------- task processing ---------- */
    /**
     * Immediate processing of a new task, in constant time Local processing, in
     * one concept only
     *
     * @param task the task to be accepted
     */
    private void immediateProcess(Task task) {
        currentTask = task; // one of the two places where this variable is set
        recorder.append("!!! Insert: " + task + "\n");
        currentTerm = task.getContent();
        currentConcept = getConcept(currentTerm); // getConcept 里包含有一个 new concept 的操作, 还有一个对应的 show3d 记录操作(insert concept).
        if (currentConcept != null) {
            Show3D.inst().append(INSERT_TASK,task);
            Show3D.inst().append(INSERT_CONCEPT, currentConcept);
            activateConcept(currentConcept, task.getBudget());
            currentConcept.directProcess(task);
        }else{
            Show3D.inst().remove(task);
        }
    }

    /* ---------- display ---------- */
    /**
     * Start display active concepts on given bagObserver, called from MainWindow.
     *
     * we don't want to expose fields concepts and novelTasks, AND we want to
     * separate GUI and inference, so this method takes as argument a 
     * {@link BagObserver} and calls {@link ConceptBag#addBagObserver(BagObserver, String)} ;
     * 
     * see design for {@link Bag} and {@link nars.gui.BagWindow}
     * in {@link Bag#addBagObserver(BagObserver, String)}
     *
     * @param bagObserver bag Observer that will receive notifications
     * @param title the window title
     */
	public void conceptsStartPlay( BagObserver<Concept> bagObserver, String title ) {
        bagObserver.setBag(concepts);
        concepts.addBagObserver(bagObserver, title);
    }

    /**
     * Display new tasks, called from MainWindow. see
     * {@link #conceptsStartPlay(BagObserver, String)}
     *
     * @param bagObserver
     * @param s the window title
     */
	public void taskBuffersStartPlay( BagObserver<Task> bagObserver, String s ) {
        bagObserver.setBag(novelTasks);
        novelTasks.addBagObserver(bagObserver, s);
    }

    /**
     * Display input/output sentence in the output channels. The only place to
     * add Objects into exportStrings. Currently only Strings are added, though
     * in the future there can be outgoing Tasks; also if exportStrings is empty
     * display the current value of timer ( exportStrings is emptied in
     * {@link ReasonerBatch#doTick()} - TODO fragile mechanism)
     *
     * @param sentence the sentence to be displayed
     * @param input whether the task is input
     */
    public void report(Sentence sentence, boolean input) {
        if (ReasonerBatch.DEBUG) {
            System.out.println("// report( clock " + reasoner.getTime()
                    + ", input " + input
                    + ", timer " + reasoner.getTimer()
                    + ", Sentence " + sentence
                    + ", exportStrings " + exportStrings);
            System.out.flush();
        }
        if (exportStrings.isEmpty()) {
            long timer = reasoner.updateTimer();
            if (timer > 0) {
                exportStrings.add(String.valueOf(timer));
            }
        }
        String s;
        if (input) {
            s = "  IN: ";
        } else {
            s = " OUT: ";
        }
        s += sentence.toStringBrief();
        exportStrings.add(s);
    }

    @Override
    public String toString() {
        return toStringLongIfNotNull(concepts, "concepts")
                + toStringLongIfNotNull(novelTasks, "novelTasks")
                + toStringIfNotNull(newTasks, "newTasks")
                + toStringLongIfNotNull(currentTask, "currentTask")
                + toStringLongIfNotNull(currentBeliefLink, "currentBeliefLink")
                + toStringIfNotNull(currentBelief, "currentBelief");
    }

    private String toStringLongIfNotNull(Bag<?> item, String title) {
        return item == null ? "" : "\n " + title + ":\n"
                + item.toStringLong();
    }

    private String toStringLongIfNotNull(Item item, String title) {
        return item == null ? "" : "\n " + title + ":\n"
                + item.toStringLong();
    }

    private String toStringIfNotNull(Object item, String title) {
        return item == null ? "" : "\n " + title + ":\n"
                + item.toString();
    }

    public AtomicInteger getTaskForgettingRate() {
        return taskForgettingRate;
    }

    public AtomicInteger getBeliefForgettingRate() {
        return beliefForgettingRate;
    }

    public AtomicInteger getConceptForgettingRate() {
        return conceptForgettingRate;
    }

    class NullInferenceRecorder implements IInferenceRecorder {

        @Override
        public void init() {
        }

        @Override
        public void show() {
        }

        @Override
        public void play() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void append(String s) {
        }

        @Override
        public void openLogFile() {
        }

        @Override
        public void closeLogFile() {
        }

        @Override
        public boolean isLogging() {
            return false;
        }
    }
}
