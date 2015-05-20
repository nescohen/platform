package lsfusion.server.logics.debug;

import lsfusion.base.Pair;

public class DebugInfo {
    public final String moduleName;
    public final int line;
    public final int offset;

    public DebugInfo(String moduleName, int line, int offset) {
        this.moduleName = moduleName;
        this.line = line;
        this.offset = offset;
    }

    public Pair<String, Integer> getModuleLine() {
        return new Pair<String, Integer>(moduleName, line);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ActionDebugInfo that = (ActionDebugInfo) o;

        return line == that.line &&
                offset == that.offset &&
                moduleName.equals(that.moduleName);
    }

    @Override
    public int hashCode() {
        int result = moduleName.hashCode();
        result = 31 * result + line;
        result = 31 * result + offset;
        return result;
    }

    @Override
    public String toString() {
        return moduleName + "(" + (line + 1) + ":" + (offset + 1) + ")";
    }
}
