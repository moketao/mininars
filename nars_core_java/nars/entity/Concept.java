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
package nars.entity;

import java.util.ArrayList;

import nars.gui3d.Show3D;
import nars.inference.BudgetFunctions;
import nars.inference.LocalRules;
import nars.inference.RuleTables;
import nars.inference.UtilityFunctions;
import nars.language.CompoundTerm;
import nars.language.Term;
import nars.main_nogui.NARSBatch;
import nars.main_nogui.Parameters;
import nars.storage.BagObserver;
import nars.storage.Memory;
import nars.storage.NullBagObserver;
import nars.storage.TaskLinkBag;
import nars.storage.TermLinkBag;

/**
 * A concept contains information associated with a term, including directly and
 * indirectly related tasks and beliefs.
 * 任务 和 知识(信仰) 的结合体.   用 term 作为 key 来标注它的唯一性.
 * <p>
 * To make sure the space will be released, the only allowed reference to a
 * concept are those in a ConceptBag. All other access go through the Term that
 * names the concept.
 * 为了确保 遗忘机能 的正常运作, 只允许通过 概念袋( ConceptBag) 来访问某个具体的概念, 取概念的时候需提供对应的 name/term/key.
 */
public final class Concept extends Item {

    /**
     * The term is the unique ID of the concept
     */
    private final Term term;
    /**
     * Task links for indirect processing
     */
    private final TaskLinkBag taskLinks;
    /**
     * Term links between the term and its components and compounds
     */
    private final TermLinkBag termLinks;
    /**
     * Link templates of TermLink, only in concepts with CompoundTerm Templates
     * are used to improve the efficiency of TermLink building
     * termlink 模板, 提高 构建 termlink 的效率.
     */
    private ArrayList<TermLink> termLinkTemplates;
    /**
     * Question directly asked about the term
     */
    private final ArrayList<Task> questions;
    /**
     * Sentences directly made about the term, with non-future tense
     */
    private final ArrayList<Sentence> beliefs;
    /**
     * Reference to the memory
     */
    Memory memory;
    /**
     * The display window
     */
    private EntityObserver entityObserver = new NullEntityObserver();


    /* ---------- constructor and initialization ---------- */
    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param tm A term corresponding to the concept
     * @param memory A reference to the memory
     */
    public Concept(Term tm, Memory memory) {
        super(tm.getName());
        term = tm;
        this.memory = memory;
        questions = new ArrayList<>();
        beliefs = new ArrayList<>();
        taskLinks = new TaskLinkBag(memory);
        termLinks = new TermLinkBag(memory);
        if (tm instanceof CompoundTerm) {
            termLinkTemplates = ((CompoundTerm) tm).prepareComponentLinks();
        }
    }

    /* ---------- direct processing of tasks ---------- */
    /**
     * Directly process a new task. Called exactly once on each task. Using
     * local information and finishing in a constant time. Provide feedback in
     * the taskBudget value of the task.
     * 分门别类地处理任务: 判断, 问题, 链接 (挑选预算值较高的 task 进行链接)
     * <p>
     * called in Memory.immediateProcess only
     * 此函数只在 Memory.immediateProcess 中调用.
     *
     * @param task The task to be processed
     */
    public void directProcess(Task task) {
        if (task.getSentence().isJudgment()) {
            processJudgment(task);
        } else {
            processQuestion(task);
        }
        if (task.getBudget().aboveThreshold()) { // still need to be processed 如果这个帅小伙耐力还不错, 何不把他骗到碗里来.
            linkToTask(task);
        }
        entityObserver.refresh(displayContent());// UI , 轮到你更新了
    }

    /**
     * To accept a new judgment as isBelief, and check for revisions and
     * solutions
     * 对判断进行修订和尝试 (trySolution 尝试解决方案)
     * @param task The task to be processed
     * @return Whether to continue the processing of the task
     */
    private void processJudgment(Task task) {
        Sentence judg = task.getSentence();
        Sentence oldBelief = evaluation(judg, beliefs);
        if (oldBelief != null) {
            Stamp newStamp = judg.getStamp();
            Stamp oldStamp = oldBelief.getStamp();
            if (newStamp.equals(oldStamp)) {
                if (task.getParentTask().getSentence().isJudgment()) {
                    task.getBudget().decPriority(0);    // duplicated task
                }   // else: activated belief
                Show3D.inst().remove(task);
                return;                                     // 如果时间差不多, 则说明重复了, 跳过, 不处理
            } else if (LocalRules.revisible(judg, oldBelief)) {                          // 如果可以修订
                memory.newStamp = Stamp.make(newStamp, oldStamp, memory.getTime());
                if (memory.newStamp != null) {
                    memory.currentBelief = oldBelief;
                    LocalRules.revision(judg, oldBelief, false, memory); // 则修订
                }
            }
        }
        if (task.getBudget().aboveThreshold()) {
            for (Task ques : questions) {
                // 尝试解决方案, 如果经验足够好(比率高/次数多)并且好实施不复杂(奥卡姆剃刀:简单就是美), 则记到任务的最佳解决方案数组中
                boolean best = LocalRules.trySolution(judg, ques, memory);
                if(!best){
                    Show3D.inst().remove(task); // 不好的则略过
                }
            }
            addToTable(judg, beliefs, Parameters.MAXIMUM_BELIEF_LENGTH);
        }else{
            Show3D.inst().remove(task); // 不好的则略过
        }
    }

    /**
     * To answer a question by existing beliefs
     * 通过已有信念集, 尝试回答一个问题.
     * @param task The task to be processed
     */
    public void processQuestion(Task task) {
        Sentence ques = task.getSentence();
        boolean newQuestion = true;
        if (questions != null) {
            for (Task t : questions) {
                Sentence q = t.getSentence();
                //这里判断: 如果存在旧问题, 并且只有两个句子全等, 才进行.
                //(不全等的情况不在这里考虑, 而是通过 Task 近似假设扩大查找范围直到句式全等, 而不在这里处理)
                if (q.getContent().equals(ques.getContent())) {// 不过一般来说, 能放进来的都是全等的, 有必要做这个比较吗? todo: 验证这个猜测.
                    ques = q;
                    newQuestion = false;
                    break;
                }
            }
        }
        if (newQuestion) {
            questions.add(task); // 如果是新问题, 则添加到旧问题列表.
        }
        if (questions.size() > Parameters.MAXIMUM_QUESTIONS_LENGTH) {
            Task remove = questions.remove(0);// FIFO 超过上限则清理, 先进先出.
            Show3D.inst().remove(remove);
        }
        Sentence newAnswer = evaluation(ques, beliefs); // 评价,评估,找到最高质量的信念.
        if (newAnswer != null) {
            boolean best = LocalRules.trySolution(newAnswer, task, memory); // 尝试用这个问题解决,如果可行,还有一系列处理.
            if( !best ) Show3D.inst().remove(task);
        } else {
            Show3D.inst().remove(task);
        }
    }

    /**
     * Link to a new task from all relevant concepts for continued processing in
     * the near future for unspecified time.
     * 把相关概念链接到一个新的 Task, 留待处理
     * <p>
     * The only method that calls the TaskLink constructor.
     *
     * @param task The task to be linked
     */
    private void linkToTask(Task task) {
        BudgetValue taskBudget = task.getBudget();
        TaskLink taskLink = new TaskLink(task, null, taskBudget);   // link type: SELF
        insertTaskLink(taskLink);
        if (term instanceof CompoundTerm) {
            if (termLinkTemplates.size() > 0) {
                BudgetValue subBudget = BudgetFunctions.distributeAmongLinks(taskBudget, termLinkTemplates.size());
                if (subBudget.aboveThreshold()) {
                    Term componentTerm;
                    Concept componentConcept;
                    for (TermLink termLink : termLinkTemplates) {
//                        if (!(task.isStructural() && (termLink.getType() == TermLink.TRANSFORM))) { // avoid circular transform
                        taskLink = new TaskLink(task, termLink, subBudget);
                        componentTerm = termLink.getTarget();
                        componentConcept = memory.getConcept(componentTerm);
                        if (componentConcept != null) {
                            componentConcept.insertTaskLink(taskLink);
                        }
//                        }
                    }
                    buildTermLinks(taskBudget);  // recursively insert TermLink
                }
            }
        }
    }

    /**
     * Add a new belief (or goal) into the table Sort the beliefs/goals by rank,
     * and remove redundant or low rank one
     * 填充新的信念或目标到 table, 并移除多余的.
     * @param newSentence The judgment to be processed
     * @param table The table to be revised
     * @param capacity The capacity of the table
     */
    private void addToTable(Sentence newSentence, ArrayList<Sentence> table, int capacity) {
        float rank1 = BudgetFunctions.rankBelief(newSentence);    // for the new isBelief
        Sentence judgment2;
        float rank2;
        int i;
        for (i = 0; i < table.size(); i++) {
            judgment2 = table.get(i);
            rank2 = BudgetFunctions.rankBelief(judgment2);
            if (rank1 >= rank2) {
                if (newSentence.equivalentTo(judgment2)) {
                    return;
                }
                table.add(i, newSentence);
                break;
            }
        }
        if (table.size() >= capacity) {
            while (table.size() > capacity) {
                table.remove(table.size() - 1);
            }
        } else if (i == table.size()) {
            table.add(newSentence);
        }
    }

    /**
     * Evaluate a query against beliefs (and desires in the future)
     * 评价,评估,找到最高质量的信念.
     * @param query The question to be processed
     * @param list The list of beliefs to be used
     * @return The best candidate belief selected
     */
    private Sentence evaluation(Sentence query, ArrayList<Sentence> list) {
        if (list == null) {
            return null;
        }
        float currentBest = 0;
        float beliefQuality;
        Sentence candidate = null;
        for (Sentence judg : list) {
            beliefQuality = LocalRules.solutionQuality(query, judg);
            if (beliefQuality > currentBest) {
                currentBest = beliefQuality;
                candidate = judg;
            }
        }
        return candidate;
    }

    /* ---------- insert Links for indirect processing ---------- */
    /**
     * Insert a TaskLink into the TaskLink bag
     * <p>
     * called only from Memory.continuedProcess
     * 把 TaskLink 加入到 TaskLink bag, 只在 Concept.directProcess 中处理.
     * @param taskLink The termLink to be inserted
     */
    public void insertTaskLink(TaskLink taskLink) {
        BudgetValue taskBudget = taskLink.getBudget();
        taskLinks.putIn(taskLink);
        memory.activateConcept(this, taskBudget);
    }

    /**
     * Recursively build TermLinks between a compound and its components
     * <p>
     * called only from Memory.continuedProcess
     *
     * @param taskBudget The BudgetValue of the task
     */
    public void buildTermLinks(BudgetValue taskBudget) {
        Term t;
        Concept concept;
        TermLink termLink1, termLink2;
        if (termLinkTemplates.size() > 0) {
            BudgetValue subBudget = BudgetFunctions.distributeAmongLinks(taskBudget, termLinkTemplates.size());
            if (subBudget.aboveThreshold()) {
                for (TermLink template : termLinkTemplates) {
                    if (template.getType() != TermLink.TRANSFORM) {
                        t = template.getTarget();
                        concept = memory.getConcept(t);
                        if (concept != null) {
                            termLink1 = new TermLink(t, template, subBudget);
                            insertTermLink(termLink1);   // this termLink to that
                            termLink2 = new TermLink(term, template, subBudget);
                            concept.insertTermLink(termLink2);   // that termLink to this
                            if (t instanceof CompoundTerm) {
                                concept.buildTermLinks(subBudget);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Insert a TermLink into the TermLink bag
     * <p>
     * called from buildTermLinks only
     *
     * @param termLink The termLink to be inserted
     */
    public void insertTermLink(TermLink termLink) {
        termLinks.putIn(termLink);
    }

    /* ---------- access local information ---------- */
    /**
     * Return the associated term, called from Memory only
     *
     * @return The associated term
     */
    public Term getTerm() {
        return term;
    }

    /**
     * Return a string representation of the concept, called in ConceptBag only
     *
     * @return The concept name, with taskBudget in the full version
     */
    @Override
    public String toString() {  // called from concept bag
        if (NARSBatch.isStandAlone()) {
            return (super.toStringBrief() + " " + key);
        } else {
            return key;
        }
    }

    /**
     * called from {@link NARSBatch}
     *
     * @return A string representation of the concept
     */
    @Override
    public String toStringLong() {
        String res = toStringBrief() + " " + key
                + toStringIfNotNull(termLinks, "termLinks")
                + toStringIfNotNull(taskLinks, "taskLinks");
        res += toStringIfNotNull(null, "questions");
        for (Task t : questions) {
            res += t.toString();
        }
        // TODO other details?
        return res;
    }

    private String toStringIfNotNull(Object item, String title) {
        return item == null ? "" : "\n " + title + ":" + item.toString();
    }

    /**
     * Recalculate the quality of the concept [to be refined to show
     * extension/intension balance]
     * 计算概念的的质量, 与平均优先级和复杂度相关
     * @return The quality value
     */
    @Override
    public float getQuality() {
        float linkPriority = termLinks.averagePriority();
        float termComplexityFactor = 1.0f / term.getComplexity();
        return UtilityFunctions.or(linkPriority, termComplexityFactor);
    }

    /**
     * Return the templates for TermLinks, only called in
     * Memory.continuedProcess
     *
     * @return The template get
     */
    public ArrayList<TermLink> getTermLinkTemplates() {
        return termLinkTemplates;
    }

    /**
     * Select a isBelief to interact with the given task in inference
     * <p>
     * get the first qualified one
     * <p>
     * only called in RuleTables.reason
     *
     * @param task The selected task
     * @return The selected isBelief
     */
    public Sentence getBelief(Task task) {
        Sentence taskSentence = task.getSentence();
        for (Sentence belief : beliefs) {
            memory.getRecorder().append(" * Selected Belief: " + belief + "\n");
            memory.newStamp = Stamp.make(taskSentence.getStamp(), belief.getStamp(), memory.getTime());
            if (memory.newStamp != null) {
                Sentence belief2 = (Sentence) belief.clone();   // will this mess up priority adjustment?
                return belief2;
            }
        }
        return null;
    }

    /* ---------- main loop ---------- */
    /**
     * An atomic step in a concept, only called in {@link Memory#processConcept}
     */
    public void fire() {
        TaskLink currentTaskLink = taskLinks.takeOut();
        if (currentTaskLink == null) {
            return;
        }
        memory.currentTaskLink = currentTaskLink;
        memory.currentBeliefLink = null;
        memory.getRecorder().append(" * Selected TaskLink: " + currentTaskLink + "\n");
        Task task = currentTaskLink.getTargetTask();
        memory.currentTask = task;  // one of the two places where this variable is set
//      memory.getRecorder().append(" * Selected Task: " + task + "\n");    // for debugging
        if (currentTaskLink.getType() == TermLink.TRANSFORM) {
            memory.currentBelief = null;
            RuleTables.transformTask(currentTaskLink, memory);  // to turn this into structural inference as below?
        } else {
            int termLinkCount = Parameters.MAX_REASONED_TERM_LINK;
//        while (memory.noResult() && (termLinkCount > 0)) {
            while (termLinkCount > 0) {
                TermLink termLink = termLinks.takeOut(currentTaskLink, memory.getTime());
                if (termLink != null) {
                    memory.getRecorder().append(" * Selected TermLink: " + termLink + "\n");
                    memory.currentBeliefLink = termLink;
                    RuleTables.reason(currentTaskLink, termLink, memory);
                    termLinks.putBack(termLink);
                    termLinkCount--;
                } else {
                    termLinkCount = 0;
                }
            }
        }
        taskLinks.putBack(currentTaskLink);
    }

    /* ---------- display ---------- */
    /**
     * Start displaying contents and links, called from ConceptWindow,
     * TermWindow or Memory.processTask only
     *
     * same design as for {@link nars.storage.Bag} and
     * {@link nars.gui.BagWindow}; see
     * {@link nars.storage.Bag#addBagObserver(BagObserver, String)}
     *
     * @param entityObserver {@link EntityObserver} to set; TODO make it a real
     * observer pattern (i.e. with a plurality of observers)
     * @param showLinks Whether to display the task links
     */
    @SuppressWarnings("unchecked")
    public void startPlay(EntityObserver entityObserver, boolean showLinks) {
        this.entityObserver = entityObserver;
        entityObserver.startPlay(this, showLinks);
        entityObserver.post(displayContent());
        if (showLinks) {
            taskLinks.addBagObserver(entityObserver.createBagObserver(), "Task Links in " + term);
            termLinks.addBagObserver(entityObserver.createBagObserver(), "Term Links in " + term);
        }
    }

    /**
     * Resume display, called from ConceptWindow only
     */
    public void play() {
        entityObserver.post(displayContent());
    }

    /**
     * Stop display, called from ConceptWindow only
     */
    public void stop() {
        entityObserver.stop();
    }

    /**
     * Collect direct isBelief, questions, and goals for display
     *
     * @return String representation of direct content
     */
    public String displayContent() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("\n  Beliefs:\n");
        if (beliefs.size() > 0) {
            for (Sentence s : beliefs) {
                buffer.append(s).append("\n");
            }
        }
        buffer.append("\n  Question:\n");
        if (questions.size() > 0) {
            for (Task t : questions) {
                buffer.append(t).append("\n");
            }
        }
        return buffer.toString();
    }

    class NullEntityObserver implements EntityObserver {

        @Override
        public void post(String str) {
        }

        @Override
        public BagObserver<TermLink> createBagObserver() {
            return new NullBagObserver<>();
        }

        @Override
        public void startPlay(Concept concept, boolean showLinks) {
        }

        @Override
        public void stop() {
        }

        @Override
        public void refresh(String message) {
        }
    }
}
