package net.donhofer.fun.threadoff.data;

public enum ThreadType {
    VIRTUAL("vThreads"),
    PLATFORM("pThreads");

    private final String label;

    ThreadType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
