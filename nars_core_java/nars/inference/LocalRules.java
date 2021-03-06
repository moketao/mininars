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
package nars.inference;

import nars.storage.Memory;
import nars.entity.*;
import nars.language.*;
import nars.io.Symbols;

/**
 * Directly process a task by a oldBelief, with only two Terms in both. In
 * matching, the new task is compared with all existing direct Tasks in that
 * Concept, to carry out:
 * <p>
 * revision: between judgments on non-overlapping evidence; revision: between
 * judgments; satisfy: between a Sentence and a Question/Goal; merge: between
 * items of the same type and stamp; conversion: between different inheritance
 * relations.
 */
public class LocalRules {

    /* -------------------- same contents -------------------- */
    /**
     * The task and belief have the same content
     * <p>
     * called in RuleTables.reason
     *
     * @param task The task
     * @param belief The belief
     * @param memory Reference to the memory
     */
    public static void match(Task task, Sentence belief, Memory memory) {
        Sentence sentence = (Sentence) task.getSentence().clone();
        if (sentence.isJudgment()) {
            if (revisible(sentence, belief)) {
                revision(sentence, belief, true, memory);
            }
        } else if (Variable.unify(Symbols.VAR_QUERY, sentence.getContent(), (Term) belief.getContent().clone())) {
            trySolution(belief, task, memory);
        }
    }

    /**
     * Check whether two sentences can be used in revision
     *
     * @param s1 The first sentence
     * @param s2 The second sentence
     * @return If revision is possible between the two sentences
     */
    public static boolean revisible(Sentence s1, Sentence s2) {
        return (s1.getContent().equals(s2.getContent()) && s1.getRevisible());
    }

    /**
     * Belief revision
     * <p>
     * called from Concept.reviseTable and match
     *
     * @param newBelief The new belief in task
     * @param oldBelief The previous belief with the same content
     * @param feedbackToLinks Whether to send feedback to the links
     * @param memory Reference to the memory
     */
    public static void revision(Sentence newBelief, Sentence oldBelief, boolean feedbackToLinks, Memory memory) {
        TruthValue newTruth = newBelief.getTruth();
        TruthValue oldTruth = oldBelief.getTruth();
        TruthValue truth = TruthFunctions.revision(newTruth, oldTruth);
        BudgetValue budget = BudgetFunctions.revise(newTruth, oldTruth, truth, feedbackToLinks, memory);
        Term content = newBelief.getContent();
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * Check if a Sentence provide a better answer to a Question or Goal
     *
     * @param belief The proposed answer
     * @param task The task to be processed
     * @param memory Reference to the memory
     */
    public static boolean trySolution(Sentence belief, Task task, Memory memory) {
        Sentence problem = task.getSentence();
        Sentence oldBest = task.getBestSolution();
        float newQ = solutionQuality(problem, belief); //
        if (oldBest != null) {
            float oldQ = solutionQuality(problem, oldBest);
            if (oldQ >= newQ) {
                return false;
            }
        }
        task.setBestSolution(belief);
        if (task.isInput()) {    // moved from Sentence
            memory.report(belief, false);
        }
        BudgetValue budget = BudgetFunctions.solutionEval(problem, belief, task, memory);
        if ((budget != null) && budget.aboveThreshold()) {
            memory.activatedTask(budget, belief, task.getParentBelief());
        }
        return true;
    }

    /**
     * Evaluate the quality of the judgment as a solution to a problem
     * 计算质量,   质量 = 长期正面经验 or 经验多少 or 长期正面经验扣减一定的复杂度.
     * @param problem A goal or question
     * @param solution The solution to be evaluated
     * @return The quality of the judgment as the solution
     */
    public static float solutionQuality(Sentence problem, Sentence solution) {
        if (problem == null) {
            return solution.getTruth().getExpectation();
        }
        TruthValue truth = solution.getTruth();
        if (problem.containQueryVar()) {   // "yes/no" question
            return truth.getExpectation() / solution.getContent().getComplexity();
        } else {                                    // "what" question or goal
            return truth.getConfidence();
        }
    }

    /* -------------------- same terms, difference relations -------------------- */
    /**
     * The task and belief match reversely
     *
     * @param memory Reference to the memory
     */
    public static void matchReverse(Memory memory) {
        Task task = memory.currentTask;
        Sentence belief = memory.currentBelief;
        Sentence sentence = task.getSentence();
        if (sentence.isJudgment()) {
            inferToSym((Sentence) sentence, belief, memory);
        } else {
            conversion(memory);
        }
    }

    /**
     * Inheritance/Implication matches Similarity/Equivalence
     *
     * @param asym A Inheritance/Implication sentence
     * @param sym A Similarity/Equivalence sentence
     * @param figure location of the shared term
     * @param memory Reference to the memory
     */
    public static void matchAsymSym(Sentence asym, Sentence sym, int figure, Memory memory) {
        if (memory.currentTask.getSentence().isJudgment()) {
            inferToAsym((Sentence) asym, (Sentence) sym, memory);
        } else {
            convertRelation(memory);
        }
    }

    /* -------------------- two-premise inference rules -------------------- */
    /**
     * {<S --> P>, <P --> S} |- <S <-> p> Produce Similarity/Equivalence from a
     * pair of reversed Inheritance/Implication
     *
     * @param judgment1 The first premise
     * @param judgment2 The second premise
     * @param memory Reference to the memory
     */
    private static void inferToSym(Sentence judgment1, Sentence judgment2, Memory memory) {
        Statement s1 = (Statement) judgment1.getContent();
        Term t1 = s1.getSubject();
        Term t2 = s1.getPredicate();
        Term content;
        if (s1 instanceof Inheritance) {
            content = Similarity.make(t1, t2, memory);
        } else {
            content = Equivalence.make(t1, t2, memory);
        }
        TruthValue value1 = judgment1.getTruth();
        TruthValue value2 = judgment2.getTruth();
        TruthValue truth = TruthFunctions.intersection(value1, value2);
        BudgetValue budget = BudgetFunctions.forward(truth, memory);
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<S <-> P>, <P --> S>} |- <S --> P> Produce an Inheritance/Implication
     * from a Similarity/Equivalence and a reversed Inheritance/Implication
     *
     * @param asym The asymmetric premise
     * @param sym The symmetric premise
     * @param memory Reference to the memory
     */
    private static void inferToAsym(Sentence asym, Sentence sym, Memory memory) {
        Statement statement = (Statement) asym.getContent();
        Term sub = statement.getPredicate();
        Term pre = statement.getSubject();
        Statement content = Statement.make(statement, sub, pre, memory);
        TruthValue truth = TruthFunctions.reduceConjunction(sym.getTruth(), asym.getTruth());
        BudgetValue budget = BudgetFunctions.forward(truth, memory);
        memory.doublePremiseTask(content, truth, budget);
    }

    /* -------------------- one-premise inference rules -------------------- */
    /**
     * {<P --> S>} |- <S --> P> Produce an Inheritance/Implication from a
     * reversed Inheritance/Implication
     *
     * @param memory Reference to the memory
     */
    private static void conversion(Memory memory) {
        TruthValue truth = TruthFunctions.conversion(memory.currentBelief.getTruth());
        BudgetValue budget = BudgetFunctions.forward(truth, memory);
        convertedJudgment(truth, budget, memory);
    }

    /**
     * {<S --> P>} |- <S <-> P> {<S <-> P>} |- <S --> P> Switch between
     * Inheritance/Implication and Similarity/Equivalence
     *
     * @param memory Reference to the memory
     */
    private static void convertRelation(Memory memory) {
        TruthValue truth = memory.currentBelief.getTruth();
        if (((Statement) memory.currentTask.getContent()).isCommutative()) {
            truth = TruthFunctions.abduction(truth, 1.0f);
        } else {
            truth = TruthFunctions.deduction(truth, 1.0f);
        }
        BudgetValue budget = BudgetFunctions.forward(truth, memory);
        convertedJudgment(truth, budget, memory);
    }

    /**
     * Convert judgment into different relation
     * <p>
     * called in MatchingRules
     *
     * @param newTruth The truth value of the new task
     * @param newBudget The budget value of the new task
     * @param memory Reference to the memory
     */
    private static void convertedJudgment(TruthValue newTruth, BudgetValue newBudget, Memory memory) {
        Statement content = (Statement) memory.currentTask.getContent();
        Statement beliefContent = (Statement) memory.currentBelief.getContent();
        Term subjT = content.getSubject();
        Term predT = content.getPredicate();
        Term subjB = beliefContent.getSubject();
        Term predB = beliefContent.getPredicate();
        Term otherTerm;
        if (Variable.containVarQuery(subjT.getName())) {
            otherTerm = (predT.equals(subjB)) ? predB : subjB;
            content = Statement.make(content, otherTerm, predT, memory);
        }
        if (Variable.containVarQuery(predT.getName())) {
            otherTerm = (subjT.equals(subjB)) ? predB : subjB;
            content = Statement.make(content, subjT, otherTerm, memory);
        }
        memory.singlePremiseTask(content, Symbols.JUDGMENT_MARK, newTruth, newBudget);
    }
}
