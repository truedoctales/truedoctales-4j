package dev.truedoctales.execution.jupiter;

import dev.truedoctales.api.annotations.StoryBook;
import dev.truedoctales.api.execute.StoryExecutionListener;
import dev.truedoctales.api.model.story.ChapterModel;
import dev.truedoctales.api.model.story.StoryBookModel;
import dev.truedoctales.api.model.story.StoryModel;
import dev.truedoctales.parser.StoryBookParserImpl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;

/// JUnit Jupiter extension provider for story-based testing.
///
/// Provides class template invocation contexts for each story in a story book, enabling
/// parameterized test execution where each story runs as a separate test class instance.
public class StoryTestProvider
    implements org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider {

  @Override
  public boolean supportsClassTemplate(org.junit.jupiter.api.extension.ExtensionContext context) {
    return true;
  }

  @Override
  public Stream<ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(
      org.junit.jupiter.api.extension.ExtensionContext context) {

    try {
      List<ClassTemplateInvocationContext> contexts = new ArrayList<>();
      Path bookPath = bookPath(context);
      StoryBookModel book = new StoryBookParserImpl(bookPath).parse();
      StoryExecutionListener storyExecutionListener = loadListener(context, book);

      for (ChapterModel chapter : book.chapters()) {
        // Track whether this is the first and last story of the chapter
        for (StoryModel story : chapter.stories()) {
          contexts.add(createInvocationContext(storyExecutionListener, book, story));
        }
      }
      return contexts.stream().onClose(storyExecutionListener::closeBook);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private org.junit.jupiter.api.extension.ClassTemplateInvocationContext createInvocationContext(
      StoryExecutionListener storyExecutionListener, StoryBookModel book, StoryModel story) {
    return new org.junit.jupiter.api.extension.ClassTemplateInvocationContext() {
      @Override
      public String getDisplayName(int invocationIndex) {
        return "Story: " + story.title();
      }

      @Override
      public List<org.junit.jupiter.api.extension.Extension> getAdditionalExtensions() {
        return List.of(
            new org.junit.jupiter.api.extension.ParameterResolver() {
              @Override
              public boolean supportsParameter(
                  org.junit.jupiter.api.extension.ParameterContext parameterContext,
                  org.junit.jupiter.api.extension.ExtensionContext extensionContext) {
                return List.of(StoryExecutionListener.class, StoryBookModel.class, Path.class)
                    .contains(parameterContext.getParameter().getType());
              }

              @Override
              public Object resolveParameter(
                  org.junit.jupiter.api.extension.ParameterContext parameterContext,
                  org.junit.jupiter.api.extension.ExtensionContext extensionContext) {
                if (parameterContext.getParameter().getType().equals(StoryBookModel.class)) {
                  return book;
                } else if (parameterContext.getParameter().getType().equals(Path.class)) {
                  return story.path();
                } else if (parameterContext
                    .getParameter()
                    .getType()
                    .equals(StoryExecutionListener.class)) {
                  return storyExecutionListener;
                } else {
                  return null;
                }
              }
            });
      }
    };
  }

  private Path bookPath(ExtensionContext context) {
    StoryBook storyBookAnnotation = context.getRequiredTestClass().getAnnotation(StoryBook.class);
    return Path.of(storyBookAnnotation.path());
  }

  private StoryExecutionListener loadListener(ExtensionContext context, StoryBookModel book) {
    StoryBook storyBookAnnotation = context.getRequiredTestClass().getAnnotation(StoryBook.class);

    List<StoryExecutionListener> listeners = new ArrayList<>();

    if (Objects.isNull(storyBookAnnotation)) {
      throw new IllegalArgumentException("StoryBook annotation is not supported here.");
    }
    Class<? extends StoryExecutionListener>[] listener = storyBookAnnotation.listener();
    for (Class<? extends StoryExecutionListener> listenerClass : listener) {
      try {
        listeners.add(listenerClass.getDeclaredConstructor().newInstance());
        listeners.forEach(l -> l.startBook(book));
      } catch (Exception e) {
        throw new IllegalStateException(
            "Failed to instantiate StoryExecutionListener: " + listenerClass.getName(), e);
      }
    }
    return new StoryExecutionListener.DelegateStoryExecutionListener(listeners);
  }
}
