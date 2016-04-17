package nc.noumea.mairie.dblintreport.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import schemacrawler.tools.executable.CommandRegistry;

public class TestCommandPlugin
{

  @Test
  public void testCommandPlugin()
    throws Exception
  {
    final CommandRegistry registry = new CommandRegistry();
    assertTrue(registry.hasCommand("dblint"));
  }

}
