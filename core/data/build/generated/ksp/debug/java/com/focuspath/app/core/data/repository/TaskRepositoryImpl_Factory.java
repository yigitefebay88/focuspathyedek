package com.focuspath.app.core.data.repository;

import com.focuspath.app.core.data.local.TaskDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class TaskRepositoryImpl_Factory implements Factory<TaskRepositoryImpl> {
  private final Provider<TaskDao> taskDaoProvider;

  private TaskRepositoryImpl_Factory(Provider<TaskDao> taskDaoProvider) {
    this.taskDaoProvider = taskDaoProvider;
  }

  @Override
  public TaskRepositoryImpl get() {
    return newInstance(taskDaoProvider.get());
  }

  public static TaskRepositoryImpl_Factory create(Provider<TaskDao> taskDaoProvider) {
    return new TaskRepositoryImpl_Factory(taskDaoProvider);
  }

  public static TaskRepositoryImpl newInstance(TaskDao taskDao) {
    return new TaskRepositoryImpl(taskDao);
  }
}
