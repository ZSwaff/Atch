package com.auriferous.atch.Callbacks;

public interface TwoVariableCallback<T, U> {
    void done(T t, U u);
}