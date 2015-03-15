package cuchaz.enigma.mapping;


public interface MemberMapping<T extends Entry> {
	T getObfEntry(ClassEntry classEntry);
	String getObfName();
}
