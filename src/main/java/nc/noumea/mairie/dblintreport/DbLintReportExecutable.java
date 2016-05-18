package nc.noumea.mairie.dblintreport;


import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.*;
import schemacrawler.schema.Catalog;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.tools.executable.BaseStagedExecutable;
import schemacrawler.tools.lint.Lint;
import schemacrawler.tools.lint.LintedCatalog;
import schemacrawler.tools.lint.LinterConfigs;
import schemacrawler.tools.lint.Linters;
import schemacrawler.tools.lint.executable.LintOptions;
import schemacrawler.tools.lint.executable.LintOptionsBuilder;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.nio.file.Files.*;
import static schemacrawler.tools.lint.LintSeverity.*;
import static sf.util.Utility.isBlank;

public class DbLintReportExecutable extends BaseStagedExecutable
{

  private static final Logger LOGGER = Logger.getLogger(DbLintReportExecutable.class.getName());

  static final String COMMAND = "dblint";

  protected DbLintReportExecutable()
  {
    super(COMMAND);
  }

  @Override
  public void executeOn(final Catalog catalog, final Connection connection) throws Exception
  {
    final LintedCatalog lintedCatalog = createLintedCatalog(catalog, connection);
    generateReport(lintedCatalog);
  }

  private LintedCatalog createLintedCatalog(final Catalog catalog, final Connection connection) throws SchemaCrawlerException
  {
    final LintOptions lintOptions = new LintOptionsBuilder().fromConfig(additionalConfiguration).toOptions();

    final LinterConfigs linterConfigs = readLinterConfigs(lintOptions);
    final Linters linters = new Linters(linterConfigs);

    final LintedCatalog lintedCatalog = new LintedCatalog(catalog, connection, linters);
    return lintedCatalog;
  }

  private void generateReport(final LintedCatalog lintedCatalog) throws IOException
  {

    DbLintResult lintResult = generateLintsResult(lintedCatalog);
    System.out.println("---"+lintedCatalog.getCollector().size());

    final Context ctx = new Context();
    ctx.setVariable("lintResult", lintResult);

    LOGGER.log(Level.INFO, "Start generate report");
    final TemplateEngine templateEngine = new TemplateEngine();
    final Charset inputCharset = outputOptions.getInputCharset();

    templateEngine.addTemplateResolver(configure(new FileTemplateResolver(), inputCharset));
    templateEngine.addTemplateResolver(configure(new ClassLoaderTemplateResolver(), inputCharset));
    templateEngine.addTemplateResolver(configure(new UrlTemplateResolver(), inputCharset));

    final String templateLocation = outputOptions.getOutputFormatValue();

    try (final Writer writer = outputOptions.openNewOutputWriter();)
    {
      templateEngine.process(templateLocation, ctx, writer);
    }
    LOGGER.log(Level.INFO, "End generate report");

  }

  private ITemplateResolver configure(final TemplateResolver templateResolver, final Charset inputEncoding)
  {
    templateResolver.setCharacterEncoding(inputEncoding.name());
    templateResolver.setTemplateMode("HTML5");
    return templateResolver;
  }

  /**
   * Obtain linter configuration from a system property
   *
   * @return LinterConfigs
   * @throws SchemaCrawlerException
   */
  private static LinterConfigs readLinterConfigs(final LintOptions lintOptions)
  {
    final LinterConfigs linterConfigs = new LinterConfigs();
    String linterConfigsFile = null;
    try
    {
      linterConfigsFile = lintOptions.getLinterConfigs();
      if (!isBlank(linterConfigsFile))
      {
        final Path linterConfigsFilePath = Paths.get(linterConfigsFile).toAbsolutePath();
        if (isRegularFile(linterConfigsFilePath) && isReadable(linterConfigsFilePath))
        {
          linterConfigs.parse(newBufferedReader(linterConfigsFilePath));
        }
        else
        {
          LOGGER.log(Level.WARNING, "Could not find linter configs file, " + linterConfigsFile);
        }
      }
      else
      {
        LOGGER.log(Level.CONFIG, "Using default linter configs");
      }

      return linterConfigs;
    }
    catch (final Exception e)
    {
      LOGGER.log(Level.WARNING, "Could not load linter configs from file, " + linterConfigsFile, e);
      return linterConfigs;
    }
  }

  DbLintResult generateLintsResult(LintedCatalog lintedCatalog){
    LOGGER.log(Level.INFO, "Start generateLintsResult");
    if (lintedCatalog == null) {
      LOGGER.log(Level.INFO, "lintedCatalog = null");
      return null;
    }

    DbLintResult result = new DbLintResult();

    Stream<Lint<?>> lints = StreamSupport.stream(lintedCatalog.getCollector().spliterator(),  false);

    // Count critical hits
    Stream<Lint<?>> hits = lints.filter(l -> l.getSeverity() == critical);
    result.setNbCriticalHit(((Long)hits.count()).intValue());

    // Count high hits
    lints = StreamSupport.stream(lintedCatalog.getCollector().spliterator(),  false);
    hits = lints.filter(l -> l.getSeverity() == high);
    result.setNbHighHit(((Long)hits.count()).intValue());

    // Count medium hits
    lints = StreamSupport.stream(lintedCatalog.getCollector().spliterator(),  false);
    hits = lints.filter(l -> l.getSeverity() == medium);
    result.setNbMediumHit(((Long)hits.count()).intValue());

    // Count low hits
    lints = StreamSupport.stream(lintedCatalog.getCollector().spliterator(),  false);
    hits = lints.filter(l -> l.getSeverity() == low);
    result.setNbLowHit(((Long)hits.count()).intValue());

    result.setJsonStringHits("[{'Severity': 'Critical', Hit: "+result.getNbCriticalHit()+"}, " +
            "{'Severity': 'High', Hit: "+result.getNbHighHit()+"}," +
            "{'Severity': 'Medium', Hit: "+result.getNbMediumHit()+"}" +
            "{'Severity': 'Low', Hit: "+result.getNbLowHit()+"}]");

    if(result.getNbCriticalHit() > 0){
      result.setGlobalScore(0);
    }
    else if(result.getNbHighHit() > 0){
      result.setGlobalScore(1);
    }
    else if(result.getNbMediumHit() > 0){
      result.setGlobalScore(2);
    }
    else if(result.getNbLowHit() > 0) {
      result.setGlobalScore(3);
    }
    else{
      result.setGlobalScore(4);
    }

    LOGGER.log(Level.INFO, "End generateLintsResult");
    return result;

  }

}
