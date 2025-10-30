package dev.langgraph.core.human;

public class HumanTaskPendingException extends Exception {
    
    private final HumanTask task;

    public HumanTaskPendingException(HumanTask task) {
        super("Human task pending: " + task.taskId());
        this.task = task;
    }

    public HumanTask getTask() {
        return task;
    }
}
