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

import nars.inference.*;
import nars.io.Symbols;
import nars.main_nogui.Parameters;

/**
 * A triple of priority (current), durability (decay), and quality (long-term average).
 * 预算值, 评估某个 item 所获得的计算资源的多少的时候, 需要用到这个值.
 * 这个值包含三个评价维度: 优先度(当前/此时的),  持久度 (稍后/持续的),  质量 (长期平均值).
 */
public class BudgetValue implements Cloneable {

    /**
     * The character that marks the two ends of a budget value 使用 $ 作为预算值的起始符号 */
    private static final char MARK = Symbols.BUDGET_VALUE_MARK;
    /** The character that separates the factors in a budget value 使用 ; 作为预算值内部因子之间的分割符 */
    private static final char SEPARATOR = Symbols.VALUE_SEPARATOR;

    /** The relative share of time resource to be allocated 优先度越高, 获得的计算时长(线程时长)就越长与 真值/复杂度/持久度 都相关 */
    protected ShortFloat priority;

	/**
	 * The percent of priority to be kept in a constant period; All priority
	 * values “decay” over time, though at different rates. Each item is given a
	 * “durability” factor in (0, 1) to specify the percentage of priority level
	 * left after each reevaluation
     * 一个百分比, 决定下一波 优先级(priority) 能保留多少.
	 */
    protected ShortFloat durability;

    /**
     * The overall (context-independent) evaluation
     * 跟当前环境无关的一个值, 这个值可上逆到主任务/根任务, 或者由研究人员在分配任务时指定.
     */
    protected ShortFloat quality;

    /** 
     * Default constructor
     */
    public BudgetValue() {
        priority = new ShortFloat(0.01f);
        durability = new ShortFloat(0.01f);
        quality = new ShortFloat(0.01f);
    }

    /** 
     * Constructor with initialization
     * @param p Initial priority
     * @param d Initial durability
     * @param q Initial quality
     */
    public BudgetValue(float p, float d, float q) {
        priority = new ShortFloat(p);
        durability = new ShortFloat(d);
        quality = new ShortFloat(q);
    }

    /**
     * Cloning constructor
     * @param v Budget value to be cloned
     */
    public BudgetValue(BudgetValue v) {
        priority = new ShortFloat(v.getPriority());
        durability = new ShortFloat(v.getDurability());
        quality = new ShortFloat(v.getQuality());
    }

    /**
     * Cloning method
     */
    @Override
    public Object clone() {
        return new BudgetValue(this.getPriority(), this.getDurability(), this.getQuality());
    }

    /**
     * Get priority value
     * @return The current priority
     */
    public float getPriority() {
        return priority.getValue();
    }

    /**
     * Change priority value
     * @param v The new priority
     */
    public void setPriority(float v) {
        priority.setValue(v);
    }

    /**
     * Increase priority value by a percentage of the remaining range
     * @param v The increasing percent
     */
    public void incPriority(float v) {
        priority.setValue(UtilityFunctions.or(priority.getValue(), v));
    }

    /**
     * Decrease priority value by a percentage of the remaining range
     * @param v The decreasing percent
     */
    public void decPriority(float v) {
        priority.setValue(UtilityFunctions.and(priority.getValue(), v));
    }

    /**
     * Get durability value
     * @return The current durability
     */
    public float getDurability() {
        return durability.getValue();
    }

    /**
     * Change durability value
     * @param v The new durability
     */
    public void setDurability(float v) {
        durability.setValue(v);
    }

    /**
     * Increase durability value by a percentage of the remaining range
     * @param v The increasing percent
     */
    public void incDurability(float v) {
        durability.setValue(UtilityFunctions.or(durability.getValue(), v));
    }

    /**
     * Decrease durability value by a percentage of the remaining range
     * @param v The decreasing percent
     */
    public void decDurability(float v) {
        durability.setValue(UtilityFunctions.and(durability.getValue(), v));
    }

    /**
     * Get quality value
     * @return The current quality
     */
    public float getQuality() {
        return quality.getValue();
    }

    /**
     * Change quality value
     * @param v The new quality
     */
    public void setQuality(float v) {
        quality.setValue(v);
    }

    /**
     * Increase quality value by a percentage of the remaining range
     * @param v The increasing percent
     */
    public void incQuality(float v) {
        quality.setValue(UtilityFunctions.or(quality.getValue(), v));
    }

    /**
     * Decrease quality value by a percentage of the remaining range
     * @param v The decreasing percent
     */
    public void decQuality(float v) {
        quality.setValue(UtilityFunctions.and(quality.getValue(), v));
    }

    /**
     * Merge one BudgetValue into another
     * @param that The other Budget
     */
    public void merge(BudgetValue that) {
        BudgetFunctions.merge(this, that);
    }

    /**
     * To summarize a BudgetValue into a single number in [0, 1]
     * @return The summary value
     */
    public float summary() {
        return UtilityFunctions.aveGeo(priority.getValue(), durability.getValue(), quality.getValue());
    }

    /**
     * Whether the budget should get any processing at all
     * <p>
     * to be revised to depend on how busy the system is
     * @return The decision on whether to process the Item
     */
    public boolean aboveThreshold() {
        return (summary() >= Parameters.BUDGET_THRESHOLD);
    }

    /**
     * Fully display the BudgetValue
     * @return String representation of the value
     */
    @Override
    public String toString() {
        return MARK + priority.toString() + SEPARATOR + durability.toString() + SEPARATOR + quality.toString() + MARK;
    }

    /**
     * Briefly display the BudgetValue
     * @return String representation of the value with 2-digit accuracy
     */
    public String toStringBrief() {
        return MARK + priority.toStringBrief() + SEPARATOR + durability.toStringBrief() + SEPARATOR + quality.toStringBrief() + MARK;
    }
}
