package com.main_project.jobflow.processors;

public interface Processor<I, O> {

    O process(I input);
}
