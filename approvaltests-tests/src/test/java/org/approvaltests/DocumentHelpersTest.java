package org.approvaltests;

import com.github.javaparser.Range;
import com.spun.util.StringUtils;
import org.approvaltests.core.Options;
import org.junit.jupiter.api.Test;
import org.lambda.query.Query;
import org.lambda.query.Queryable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;

public class DocumentHelpersTest
{
  @Test
  void listAllVerifyFunctions()
  {
    Queryable<Method> methods = getAllVerifyFunctionsWithOptions(OptionsTest.getApprovalClasses());
    Queryable<String> lines = methods.select(m -> String.format("%s. [%s ](%s) (%s)",
        m.getDeclaringClass().getSimpleName(), m.getName(), getLink(m), showParameters(m)))
        .orderBy(s -> s.replaceAll("#L\\d+-L\\d+", ""));
    Approvals.verifyAll("", lines, l -> String.format(" * %s  ", l), new Options().forFile().withExtension(".md"));
  }
  // why is this calculation of the modifier necessary?
  @Test
  void testLineNumbers()
  {
    getLink(Query.first(Approvals.class.getMethods(),
        m -> m.getName().equals("verifyAll") && m.getParameterTypes()[0].equals(Object[].class)));
  }
  public static String showParameters(Method m)
  {
    return StringUtils.join(Query.select(m.getParameters(), p -> String.format("%s", p.getType().getSimpleName())),
        ",");
  }
  public static String getLink(Method m)
  {
    String baseUrl = "https://github.com/approvals/ApprovalTests.Java/blob/master/approvaltests/src/main/java";
    String file = m.getDeclaringClass().getName().replace('.', '/') + ".java";
    Range methodLines = ParsingFilesTest.getMethodLines(m);
    int start = methodLines.begin.line;
    int end = methodLines.end.line;
    return String.format("%s/%s#L%s-L%s", baseUrl, file, start, end);
  }
  private static String getRegexForParameter(Method m)
  {
    // https://github.com/jboss-javassist/javassist
    //        Class<?> parameterType = m.getParameterTypes()[0];
    Type typeParameters = m.getGenericParameterTypes()[0];
    String name = Query.last(StringUtils.split(typeParameters.getTypeName(), "."));
    return name.replaceAll("\\[\\]", "");
  }
  private static int countNewLines(String code, int untilIndex)
  {
    return code.substring(0, untilIndex).split("\n").length;
  }
  private boolean isStartOfMethod(Method m, String line)
  {
    String pattern = "void " + m.getName() + "(" + m.getParameterTypes()[0].getSimpleName();
    return line.contains(pattern) && line.contains(", Options");
  }
  private Queryable<Method> getAllVerifyFunctionsWithOptions(List<Class<?>> approvalClasses)
  {
    Queryable<Method> methods = new Queryable<>(Method.class);
    for (Class c : approvalClasses)
    {
      Queryable<Method> declaredMethods = Queryable.as(c.getDeclaredMethods());
      Queryable<Method> methodList = declaredMethods.where(m -> m.getName().startsWith("verify")
          && Modifier.isPublic(m.getModifiers()) && !m.isAnnotationPresent(Deprecated.class));
      List<Method> methodsWithOptions = methodList.where(m -> !OptionsTest.isOptionsPresent(m));
      methods.addAll(methodsWithOptions);
    }
    return methods;
  }
}
