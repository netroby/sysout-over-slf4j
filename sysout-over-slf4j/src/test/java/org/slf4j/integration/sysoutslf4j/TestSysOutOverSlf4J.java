package org.slf4j.integration.sysoutslf4j;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.logging.ConsoleHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.SimpleLayout;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.sysoutslf4j.SysOutOverSLF4JTestCase;
import org.slf4j.sysoutslf4j.common.SystemOutput;
import org.slf4j.sysoutslf4j.context.SysOutOverSLF4J;
import org.slf4j.testutils.Assert;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.layout.EchoLayout;

public class TestSysOutOverSlf4J extends SysOutOverSLF4JTestCase {

	private static final String PACKAGE_NAME = StringUtils.substringBeforeLast(TestSysOutOverSlf4J.class.getName(), ".");
	private static final Marker STACKTRACE_MARKER = MarkerFactory.getMarker("stacktrace");

	@Before
	public void setUp() {
		log.setLevel(Level.INFO);
	}

	@After
	public void unregisterLoggingSystemPackage() {
		SysOutOverSLF4J.unregisterLoggingSystem(PACKAGE_NAME);
	}

	@Test
	public void systemOutNoLongerGoesToSystemOut() throws Exception {
		OutputStream sysOutMock = setUpMockSystemOutput(SystemOutput.SYSOUT);
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		System.out.println("Hello again");
		
		assertEquals("", sysOutMock.toString());
	}

	private OutputStream setUpMockSystemOutput(SystemOutput systemOutput) {
		OutputStream sysOutMock = new ByteArrayOutputStream();
		systemOutput.set(new PrintStream(sysOutMock));
		return sysOutMock;
	}
	
	@Test
	public void systemOutLoggedAsInfo() throws Exception {
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		System.out.println("Hello World");
		
		assertExpectedLoggingEvent(appender.list.get(0), "Hello World", Level.INFO, null);
	}
	
	@Test
	public void systemErrLoggedAsError() throws Exception {
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		System.err.println("Hello World");
		
		assertExpectedLoggingEvent(appender.list.get(0), "Hello World", Level.ERROR, null);
	}
	
	@Test
	public void logBackConsoleAppenderStillLogsToConsole() throws Exception {
		OutputStream sysOutMock = setUpMockSystemOutput(SystemOutput.SYSOUT);
		configureLogBackConsoleAppender();
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		log.info("Should reach the old sysout");
		
		assertEquals("[INFO] Should reach the old sysout" + CoreConstants.LINE_SEPARATOR, sysOutMock.toString());
	}

	private void configureLogBackConsoleAppender() {
		log.detachAndStopAllAppenders();
		Appender<ILoggingEvent> app = new ConsoleAppender<ILoggingEvent>();
		app.setLayout(new EchoLayout<ILoggingEvent>());
		app.setContext(lc);
		app.start();
		log.addAppender(app);
	}
	
	@Test
	public void juliConsoleAppenderStillLogsToConsole() throws Exception {
		OutputStream newSysErr = setUpMockSystemOutput(SystemOutput.SYSERR);
		java.util.logging.Logger log = configureJuliLoggerToUseConsoleHandler();
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

		log.info("Should reach the old syserr");
		
		assertTrue(newSysErr.toString().contains("INFO: Should reach the old syserr"));
	}

	private java.util.logging.Logger configureJuliLoggerToUseConsoleHandler() {
		java.util.logging.Logger log = java.util.logging.Logger.getLogger(getClass().getCanonicalName());
		log.addHandler(new ConsoleHandler());
		return log;
	}
	
	@Test
	public void log4JConsoleAppenderStillLogsToConsole() throws Exception {
		OutputStream sysOutMock = setUpMockSystemOutput(SystemOutput.SYSOUT);
		org.apache.log4j.Logger log = configureLog4jLoggerToUseConsoleAppender();
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		log.info("Should reach the old sysout");
		
		assertEquals("INFO - Should reach the old sysout" + CoreConstants.LINE_SEPARATOR, sysOutMock.toString());
	}

	private org.apache.log4j.Logger configureLog4jLoggerToUseConsoleAppender() {
		org.apache.log4j.Logger log = org.apache.log4j.Logger.getRootLogger();
		log.setLevel(org.apache.log4j.Level.INFO);
		log.removeAllAppenders();
		log.addAppender(new org.apache.log4j.ConsoleAppender(new SimpleLayout()));
		return log;
	}
	
	@Test
	public void printMethodsAreLogged() {
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		System.out.print("Hello World");
		System.out.print(true);
		System.out.print(1);
		System.out.print('c');
		System.out.print('\n');
		
		assertExpectedLoggingEvent(appender.list.get(0), "Hello Worldtrue1c", Level.INFO, null);
	}
	
	private static final int FOUR = 4;
	
	@Test
	public void appendMethodsAreLogged() {
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		System.out.append('c');
		System.out.append("Hello");
		System.out.append("Hello", 0, FOUR);
		System.out.println();
		
		assertExpectedLoggingEvent(appender.list.get(0), "cHelloHell", Level.INFO, null);
	}
	
	@Test
	public void formatMethodsAreLogged() {
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		System.out.format("Hello %1$s", "World");
		System.out.format(Locale.getDefault(), "Disciples: %1$s\r\n", 12);
		
		assertExpectedLoggingEvent(appender.list.get(0), "Hello WorldDisciples: 12", Level.INFO, null);
	}
	
	@Test
	public void testPrintfMethods() {
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		System.out.printf("Hello %1$s", "World");
		System.out.printf(Locale.getDefault(), "Disciples: %1$s\r\n", 12);
		
		assertEquals("Hello WorldDisciples: 12", appender.list.get(0).getMessage());
	}
	
	@Test
	public void testPrintStackTrace() {
		
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		Exception exception = new Exception();
		exception.printStackTrace();
		
		assertExpectedStackTraceLoggingEvents(exception, Level.ERROR);
	}

	private void assertExpectedStackTraceLoggingEvents(Exception exception, Level level) {
		assertExpectedLoggingEvent(appender.list.get(0), exception.toString(), level, STACKTRACE_MARKER);
		StackTraceElement[] stackTrace = exception.getStackTrace();
		for (int i = 0; i < stackTrace.length; i++) {
			assertExpectedLoggingEvent(appender.list.get(i + 1), "\tat " + stackTrace[i].toString(), level, STACKTRACE_MARKER);
		}
		assertEquals(exception.getStackTrace().length + 1, appender.list.size());
	}
	
	@Test
	public void testPrintStackTraceWithSysOut() {
		
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		Exception exception = new Exception();
		exception.printStackTrace(System.out);
		
		assertExpectedStackTraceLoggingEvents(exception, Level.INFO);
	}
	
	private void assertExpectedLoggingEvent(ILoggingEvent loggingEvent, String message, Level level, Marker marker) {
		Assert.assertExpectedLoggingEvent(loggingEvent, message, level, marker, getClass().getName());
	}
	
	@Test
	public void testInnerClassLoggedAsOuterClass() {
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		(new Runnable() {
			public void run() {
				System.out.println("From inner class");
			}
		}).run();
		
		Assert.assertExpectedLoggingEvent(appender.list.get(0), "From inner class", Level.INFO, null, getClass().getName());
	}

	@Test
	public void testRegisteredLoggingSystemCanStillGetToConsole() {
		OutputStream sysOutMock = setUpMockSystemOutput(SystemOutput.SYSOUT);
		SysOutOverSLF4J.registerLoggingSystem(PACKAGE_NAME);
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		
		System.out.println("Should reach console");
		
		assertEquals("Should reach console" + CoreConstants.LINE_SEPARATOR, sysOutMock.toString());
	}
}