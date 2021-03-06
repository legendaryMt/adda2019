package us.lsi.tiposrecursivos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import us.lsi.common.Maps2;


public class NaryExp<T,R> extends Exp<R> {

	private List<Exp<T>> ops = null;
	private NaryOperator<T,R> accumulator;
	private int id;
	private static int lastId = 0;
	private Boolean flag = false;

	NaryExp(List<Exp<T>> ops, NaryOperator<T,R> accumulator) {
		super();
		this.ops = ops;
		this.accumulator = accumulator;
		this.id = lastId;
		lastId++;
	}

	@Override
	public R getValue() {	
		return ops.stream().map(x->x.getValue()).collect(this.accumulator.getCode());
	}

	@Override
	public NaryExp<T,R> copy() {
		List<Exp<T>> ops = this.ops.stream().map(x->x.copy()).collect(Collectors.toList());
		return Exp.nary(ops, this.accumulator);
	}


	public List<Exp<T>> getOps() {
		return ops;
	}

	
	@Override
	public String toString() {
		return " "+accumulator.getName()+ops.stream().map(x->x.toString()).collect(
				Collectors.joining(",",
								   "(",
								   ")"));
	}

	public void setOps(List<Exp<T>> ops) {
		this.ops = ops;
	}

	@Override
	public Integer getArity() {
		return this.ops.size();
	}

	public Operator getOperator() {
		return accumulator;
	}
	
	
	@Override
	public Boolean isNary() {
		return true;
	}
	@Override
	public Boolean isConstant() {
		return this.ops.stream().allMatch(x->x.isConstant());
	}
	
	@Override
	public Exp<R> simplify() {
		Exp<R> r;
		if(this.isConstant()){
			r = Exp.constant(this.getValue());
		} else {
			List<Exp<T>> ls = this.ops.stream().map(x->x.simplify()).collect(Collectors.toList());
			r = Exp.nary(ls, accumulator);
		}
		return r;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accumulator == null) ? 0 : accumulator.hashCode());
		result = prime * result + ((ops == null) ? 0 : ops.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof NaryExp))
			return false;
		NaryExp<?,?> other = (NaryExp<?,?>) obj;
		if (accumulator == null) {
			if (other.accumulator != null)
				return false;
		} else if (!accumulator.equals(other.accumulator))
			return false;
		if (ops == null) {
			if (other.ops != null)
				return false;
		} else if (!ops.equals(other.ops))
			return false;
		return true;
	}

	@Override
	public Boolean match(Exp<?> exp) {
		Boolean r = false;
		if(exp.isFree()){
			exp.<R>asFree().setExp(this);
			r = true;
		} else if(exp.isNary()){
			NaryExp<?,?> t = exp.asNary();
			if(this.ops.size() == t.ops.size()) {
				r = IntStream.range(0,this.ops.size())
						.allMatch(i->this.ops.get(i).match(t.ops.get(i)));
			}
		}
		return r;
	}

	@Override
	protected String getId() {
		return "NE_"+id;
	}

	@Override
	protected void toDOT(String file) {
		if (!flag) {
			String s1 = "    \"%s\" [label=\"%s\"];";
			Element.getFile()
					.println(
							String.format(s1, getId(),
									this.accumulator.getName()));
			String s2 = "    \"%s\" -> \"%s\"  [label=\"%d\"];";
			for (int i = 0; i < ops.size(); i++) {
				Element.getFile().println(
						String.format(s2, getId(), ops.get(i).getId(), i));
				ops.get(i).toDOT(file);
			}
		}
		flag = true;
	}
	
	@Override
	protected void setFlagFalse() {
		flag = false;
		ops.stream().forEach(x->x.setFlagFalse());
	}

	@Override
	public ExpType getType() {
		return accumulator.getOperatorType()[1];
	}
	
	@Override
	protected Map<String, Exp<Object>> vars() {
		Map<String, Exp<Object>> r = new HashMap<>();
		ops.stream().forEach(x->r.putAll(x.vars()));
		return r;
	}
	
	@Override
	public Integer getPriority() {
		return 12;
	}
	
	@Override
	public Exp<Object> ifMatchTransform(Exp<Object> pattern, Map<String,Exp<Object>> vars,String patternResult) {
		List<Exp<Object>> r1 = ops.stream()
				.map(x->x.ifMatchTransform(pattern, vars, patternResult))
				.collect(Collectors.toList());
		@SuppressWarnings("unchecked")
		Exp<Object> r = Exp.nary(r1,(NaryOperator<Object,Object>)this.accumulator);
		Exp<Object> copy = pattern.copy();
		if(r.match(copy)){
			Map<String,Exp<Object>> m = copy.getVars();
			Map<String,Exp<Object>> m2 = 
					Maps2.<String,Exp<Object>,Exp<Object>>newHashMap(m, 
							x->x.isFree()?x.asFree().getExp():x);
			m2.putAll(vars);
			r = Exp.parse(patternResult,m2);
		}
		return r;
	}
	
	@Override
	public Boolean isPattern() {
		return this.ops.stream().anyMatch(x->x.isPattern());
	}
	
}
