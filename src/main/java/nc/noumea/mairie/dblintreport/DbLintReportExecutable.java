package nc.noumea.mairie.dblintreport;


import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.*;
import schemacrawler.schema.Catalog;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.tools.executable.BaseStagedExecutable;
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

import static java.nio.file.Files.*;
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
    final Context ctx = new Context();
    ctx.setVariable("catalog", lintedCatalog);

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

}
