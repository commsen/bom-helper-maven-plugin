package com.commsen.maven.plugin.bomhelper;

class Gav implements Comparable<Gav>{
	public String g;
	public String a;
	public String v;

	
	public Gav(String g, String a, String v) {
		this.g = g;
		this.a = a;
		this.v = v;
	}


	@Override
	public int compareTo(Gav other) {
	    int cmp = g.compareTo(other.g);
	    if (cmp == 0)
	        cmp = a.compareTo(other.a);
	    if (cmp == 0)
	        cmp = v.compareTo(other.v);
	    return cmp;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((g == null) ? 0 : g.hashCode());
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Gav other = (Gav) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (g == null) {
			if (other.g != null)
				return false;
		} else if (!g.equals(other.g))
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "Gav [g=" + g + ", a=" + a + ", v=" + v + "]";
	}
}