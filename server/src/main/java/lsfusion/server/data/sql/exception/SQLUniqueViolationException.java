package lsfusion.server.data.sql.exception;

import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;

public class SQLUniqueViolationException extends SQLHandledException {
    
    private final boolean possibleRaceCondition;

    public SQLUniqueViolationException(boolean possibleRaceCondition) {
        this.possibleRaceCondition = possibleRaceCondition;
    }
    
    public SQLUniqueViolationException raceCondition() {
        assert !possibleRaceCondition;

        return new SQLUniqueViolationException(true);
    }

    @Override
    public boolean repeatApply(SQLSession sql, OperationOwner owner, int attempts) {
        if(attempts > Settings.get().getTooMuchAttempts())
            return false;

        return possibleRaceCondition;
    }

    @Override
    public boolean willDefinitelyBeHandled() {
        return false;
    }

    @Override
    public String getMessage() {
        return "UNIQUE_VIOLATION" + (possibleRaceCondition ? " POS_RACE" : "") ;
    }

    @Override
    public String getDescription(boolean wholeTransaction) {
        return "u";
    }
}
