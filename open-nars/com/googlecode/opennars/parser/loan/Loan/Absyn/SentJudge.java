package com.googlecode.opennars.parser.loan.Loan.Absyn; // Java Package generated by the BNF Converter.

public class SentJudge extends Sentence {
  public final Stm stm_;
  public final TruthValue truthvalue_;

  public SentJudge(Stm p1, TruthValue p2) { stm_ = p1; truthvalue_ = p2; }

  public <R,A> R accept(com.googlecode.opennars.parser.loan.Loan.Absyn.Sentence.Visitor<R,A> v, A arg) { return v.visit(this, arg); }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof com.googlecode.opennars.parser.loan.Loan.Absyn.SentJudge) {
      com.googlecode.opennars.parser.loan.Loan.Absyn.SentJudge x = (com.googlecode.opennars.parser.loan.Loan.Absyn.SentJudge)o;
      return this.stm_.equals(x.stm_) && this.truthvalue_.equals(x.truthvalue_);
    }
    return false;
  }

  public int hashCode() {
    return 37*(this.stm_.hashCode())+this.truthvalue_.hashCode();
  }


}