package org.apache.maven.shared.invoker;

import junit.framework.TestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class MavenCommandLineBuilderTest
    extends TestCase
{
    private List toDelete = new ArrayList();

    private Properties sysProps;

    public void testShouldFailToSetLocalRepoLocationGloballyWhenItIsAFile()
        throws IOException
    {
        logTestStart();

        File lrd = File.createTempFile( "workdir-test", "file" ).getCanonicalFile();

        toDelete.add( lrd );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setLocalRepositoryDirectory( lrd );

        Commandline cli = new Commandline();

        try
        {
            tcb.setEnvironmentPaths( new DefaultInvocationRequest(), cli );
            fail( "Should not set local repo location to point to a file." );
        }
        catch ( IllegalArgumentException e )
        {
        }
    }

    public void testShouldFailToSetLocalRepoLocationFromRequestWhenItIsAFile()
        throws IOException
    {
        logTestStart();

        File lrd = File.createTempFile( "workdir-test", "file" ).getCanonicalFile();

        toDelete.add( lrd );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();

        Commandline cli = new Commandline();

        try
        {
            tcb.setEnvironmentPaths( new DefaultInvocationRequest().setLocalRepositoryDirectory( lrd ), cli );
            fail( "Should not set local repo location to point to a file." );
        }
        catch ( IllegalArgumentException e )
        {
        }
    }

    public void testShouldSetLocalRepoLocationGlobally()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();

        File lrd = new File( tmpDir, "workdir" ).getCanonicalFile();

        lrd.mkdirs();
        toDelete.add( lrd );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setLocalRepositoryDirectory( lrd );

        Commandline cli = new Commandline();

        tcb.setEnvironmentPaths( new DefaultInvocationRequest(), cli );

        assertArgumentsPresent( Collections.singleton( "-Dmaven.repo.local=" + lrd.getPath() ), cli );
    }

    public void testShouldSetLocalRepoLocationFromRequest()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();

        File lrd = new File( tmpDir, "workdir" ).getCanonicalFile();

        lrd.mkdirs();
        toDelete.add( lrd );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();

        Commandline cli = new Commandline();

        tcb.setEnvironmentPaths( new DefaultInvocationRequest().setLocalRepositoryDirectory( lrd ), cli );

        assertArgumentsPresent( Collections.singleton( "-Dmaven.repo.local=" + lrd.getPath() ), cli );
    }

    public void testRequestProvidedLocalRepoLocationShouldOverrideGlobal()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();

        File lrd = new File( tmpDir, "workdir" ).getCanonicalFile();
        File glrd = new File( tmpDir, "global/workdir" ).getCanonicalFile();

        lrd.mkdirs();
        glrd.mkdirs();

        toDelete.add( lrd );
        toDelete.add( glrd );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setLocalRepositoryDirectory( glrd );

        Commandline cli = new Commandline();

        tcb.setEnvironmentPaths( new DefaultInvocationRequest().setLocalRepositoryDirectory( lrd ), cli );

        assertArgumentsPresent( Collections.singleton( "-Dmaven.repo.local=" + lrd.getPath() ), cli );
    }

    public void testShouldSetWorkingDirectoryGlobally()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();

        File wd = new File( tmpDir, "workdir" );

        wd.mkdirs();

        toDelete.add( wd );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setWorkingDirectory( wd );

        Commandline cli = new Commandline();

        tcb.setEnvironmentPaths( new DefaultInvocationRequest(), cli );

        assertEquals( cli.getWorkingDirectory(), wd );
    }

    public void testShouldSetWorkingDirectoryFromRequest()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();

        File wd = new File( tmpDir, "workdir" );

        wd.mkdirs();

        toDelete.add( wd );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        InvocationRequest req = new DefaultInvocationRequest();
        req.setBaseDirectory( wd );

        Commandline cli = new Commandline();

        tcb.setEnvironmentPaths( req, cli );

        assertEquals( cli.getWorkingDirectory(), wd );
    }

    public void testRequestProvidedWorkingDirectoryShouldOverrideGlobal()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();

        File wd = new File( tmpDir, "workdir" );
        File gwd = new File( tmpDir, "global/workdir" );

        wd.mkdirs();
        gwd.mkdirs();

        toDelete.add( wd );
        toDelete.add( gwd );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setWorkingDirectory( gwd );

        InvocationRequest req = new DefaultInvocationRequest();
        req.setBaseDirectory( wd );

        Commandline cli = new Commandline();

        tcb.setEnvironmentPaths( req, cli );

        assertEquals( cli.getWorkingDirectory(), wd );
    }

    public void testShouldUseSystemOutLoggerWhenNoneSpecified()
        throws Exception
    {
        logTestStart();
        setupTempMavenHomeIfMissing();

        TestCommandLineBuilder tclb = new TestCommandLineBuilder();
        tclb.checkRequiredState();
    }

    private File setupTempMavenHomeIfMissing()
        throws Exception
    {
        String mavenHome = System.getProperty( "maven.home" );
        
        File appDir = null;

        if ( mavenHome == null || !new File( mavenHome ).exists() )
        {
            File tmpDir = getTempDir();
            appDir = new File( tmpDir, "invoker-tests/maven-home" );

            File binDir = new File( appDir, "bin" );

            binDir.mkdirs();
            toDelete.add( appDir );

            if ( Os.isFamily( "windows" ) )
            {
                createDummyFile( binDir, "mvn.bat" );
            }
            else
            {
                createDummyFile( binDir, "mvn" );
            }

            Properties props = System.getProperties();
            props.setProperty( "maven.home", appDir.getCanonicalPath() );

            System.setProperties( props );
        }
        else
        {
            appDir = new File( mavenHome );
        }
        
        return appDir;
    }

    public void testShouldFailIfLoggerSetToNull()
    {
        logTestStart();

        TestCommandLineBuilder tclb = new TestCommandLineBuilder();
        tclb.setLogger( null );

        try
        {
            tclb.checkRequiredState();
            fail( "Should not allow execution to proceed when logger is missing." );
        }
        catch ( IllegalStateException e )
        {
        }
    }

    public void testShouldFindDummyMavenExecutable()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();

        File base = new File( tmpDir, "invoker-tests" );

        File dummyMavenHomeBin = new File( base, "dummy-maven-home/bin" );

        dummyMavenHomeBin.mkdirs();

        toDelete.add( base );

        File check;
        if ( Os.isFamily( "windows" ) )
        {
            check = createDummyFile( dummyMavenHomeBin, "mvn.bat" );
        }
        else
        {
            check = createDummyFile( dummyMavenHomeBin, "mvn" );
        }

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setMavenHome( dummyMavenHomeBin.getParentFile() );

        File mavenExe = tcb.findMavenExecutable();

        assertEquals( check.getCanonicalPath(), mavenExe.getCanonicalPath() );
    }

    public void testShouldSetBatchModeFlagFromRequest()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb.setFlags( new DefaultInvocationRequest().setInteractive( false ), cli );

        assertArgumentsPresent( Collections.singleton( "-B" ), cli );
    }

    public void testShouldSetOfflineFlagFromRequest()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb.setFlags( new DefaultInvocationRequest().setOffline( true ), cli );

        assertArgumentsPresent( Collections.singleton( "-o" ), cli );
    }

    public void testShouldSetUpdateSnapshotsFlagFromRequest()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb.setFlags( new DefaultInvocationRequest().setUpdateSnapshots( true ), cli );

        assertArgumentsPresent( Collections.singleton( "-U" ), cli );
    }

    public void testShouldSetDebugFlagFromRequest()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb.setFlags( new DefaultInvocationRequest().setDebug( true ), cli );

        assertArgumentsPresent( Collections.singleton( "-X" ), cli );
    }

    public void testShouldSetErrorFlagFromRequest()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb.setFlags( new DefaultInvocationRequest().setShowErrors( true ), cli );

        assertArgumentsPresent( Collections.singleton( "-e" ), cli );
    }

    public void testDebugOptionShouldMaskShowErrorsOption()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb.setFlags( new DefaultInvocationRequest().setDebug( true ).setShowErrors( true ), cli );

        assertArgumentsPresent( Collections.singleton( "-X" ), cli );
        assertArgumentsNotPresent( Collections.singleton( "-e" ), cli );
    }

    public void testShouldSetStrictChecksumPolityFlagFromRequest()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb.setFlags( new DefaultInvocationRequest().setGlobalChecksumPolicy( InvocationRequest.CHECKSUM_POLICY_FAIL ),
                      cli );

        assertArgumentsPresent( Collections.singleton( "-C" ), cli );
    }

    public void testShouldSetLaxChecksumPolicyFlagFromRequest()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb.setFlags( new DefaultInvocationRequest().setGlobalChecksumPolicy( InvocationRequest.CHECKSUM_POLICY_WARN ),
                      cli );

        assertArgumentsPresent( Collections.singleton( "-c" ), cli );
    }

    public void testShouldSetFailAtEndFlagFromRequest()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb.setReactorBehavior( new DefaultInvocationRequest()
            .setFailureBehavior( InvocationRequest.REACTOR_FAIL_AT_END ), cli );

        assertArgumentsPresent( Collections.singleton( "-fae" ), cli );
    }

    public void testShouldSetFailNeverFlagFromRequest()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb.setReactorBehavior( new DefaultInvocationRequest()
            .setFailureBehavior( InvocationRequest.REACTOR_FAIL_NEVER ), cli );

        assertArgumentsPresent( Collections.singleton( "-fn" ), cli );
    }

    public void testShouldUseDefaultOfFailFastWhenSpecifiedInRequest()
    {
        logTestStart();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        Commandline cli = new Commandline();

        tcb
            .setReactorBehavior( new DefaultInvocationRequest()
                .setFailureBehavior( InvocationRequest.REACTOR_FAIL_FAST ), cli );

        Set banned = new HashSet();
        banned.add( "-fae" );
        banned.add( "-fn" );

        assertArgumentsNotPresent( banned, cli );
    }

    public void testShouldSpecifyFileOptionUsingNonStandardPomFileLocation()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();
        File base = new File( tmpDir, "invoker-tests" );

        toDelete.add( base );

        File projectDir = new File( base, "file-option-nonstd-pom-file-location" ).getCanonicalFile();

        projectDir.mkdirs();

        File pomFile = createDummyFile( projectDir, "non-standard-pom.xml" ).getCanonicalFile();

        Commandline cli = new Commandline();

        InvocationRequest req = new DefaultInvocationRequest().setPomFile( pomFile );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir, cli.getWorkingDirectory() );

        Set args = new HashSet();
        args.add( "-f" );
        args.add( "non-standard-pom.xml" );

        assertArgumentsPresent( args, cli );
    }

    public void testShouldSpecifyFileOptionUsingNonStandardPomInBasedir()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();
        File base = new File( tmpDir, "invoker-tests" );

        toDelete.add( base );

        File projectDir = new File( base, "file-option-nonstd-basedir" ).getCanonicalFile();

        projectDir.mkdirs();

        File basedir = createDummyFile( projectDir, "non-standard-pom.xml" ).getCanonicalFile();

        Commandline cli = new Commandline();

        InvocationRequest req = new DefaultInvocationRequest().setBaseDirectory( basedir );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir, cli.getWorkingDirectory() );

        Set args = new HashSet();
        args.add( "-f" );
        args.add( "non-standard-pom.xml" );

        assertArgumentsPresent( args, cli );
    }

    public void testShouldNotSpecifyFileOptionUsingStandardPomFileLocation()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();
        File base = new File( tmpDir, "invoker-tests" );

        toDelete.add( base );

        File projectDir = new File( base, "std-pom-file-location" ).getCanonicalFile();

        projectDir.mkdirs();

        File pomFile = createDummyFile( projectDir, "pom.xml" ).getCanonicalFile();

        Commandline cli = new Commandline();

        InvocationRequest req = new DefaultInvocationRequest().setPomFile( pomFile );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir, cli.getWorkingDirectory() );

        Set args = new HashSet();
        args.add( "-f" );
        args.add( "pom.xml" );

        assertArgumentsNotPresent( args, cli );
    }

    public void testShouldNotSpecifyFileOptionUsingStandardPomInBasedir()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();
        File base = new File( tmpDir, "invoker-tests" );

        toDelete.add( base );

        File projectDir = new File( base, "std-basedir-is-pom-file" ).getCanonicalFile();

        projectDir.mkdirs();

        File basedir = createDummyFile( projectDir, "pom.xml" ).getCanonicalFile();

        Commandline cli = new Commandline();

        InvocationRequest req = new DefaultInvocationRequest().setBaseDirectory( basedir );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir, cli.getWorkingDirectory() );

        Set args = new HashSet();
        args.add( "-f" );
        args.add( "pom.xml" );

        assertArgumentsNotPresent( args, cli );
    }

    public void testShouldUseDefaultPomFileWhenBasedirSpecifiedWithoutPomFileName()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();
        File base = new File( tmpDir, "invoker-tests" );

        toDelete.add( base );

        File projectDir = new File( base, "std-basedir-no-pom-filename" ).getCanonicalFile();

        projectDir.mkdirs();

        Commandline cli = new Commandline();

        InvocationRequest req = new DefaultInvocationRequest().setBaseDirectory( projectDir );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir, cli.getWorkingDirectory() );

        Set args = new HashSet();
        args.add( "-f" );
        args.add( "pom.xml" );

        assertArgumentsNotPresent( args, cli );
    }

    public void testShouldSpecifyPomFileWhenBasedirSpecifiedWithPomFileName()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();
        File base = new File( tmpDir, "invoker-tests" );

        toDelete.add( base );

        File projectDir = new File( base, "std-basedir-with-pom-filename" ).getCanonicalFile();

        projectDir.mkdirs();

        Commandline cli = new Commandline();

        InvocationRequest req = new DefaultInvocationRequest().setBaseDirectory( projectDir )
            .setPomFileName( "non-standard-pom.xml" );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setEnvironmentPaths( req, cli );
        tcb.setPomLocation( req, cli );

        assertEquals( projectDir, cli.getWorkingDirectory() );

        Set args = new HashSet();
        args.add( "-f" );
        args.add( "non-standard-pom.xml" );

        assertArgumentsPresent( args, cli );
    }

    public void testShouldSpecifyCustomSettingsLocationFromRequest()
        throws Exception
    {
        logTestStart();

        File tmpDir = getTempDir();
        File base = new File( tmpDir, "invoker-tests" );

        toDelete.add( base );

        File projectDir = new File( base, "custom-settings" ).getCanonicalFile();

        projectDir.mkdirs();

        File settingsFile = createDummyFile( projectDir, "settings.xml" );

        Commandline cli = new Commandline();

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setSettingsLocation( new DefaultInvocationRequest().setUserSettingsFile( settingsFile ), cli );

        Set args = new HashSet();
        args.add( "-s" );
        args.add( settingsFile.getCanonicalPath() );

        assertArgumentsPresent( args, cli );
    }

    public void testShouldSpecifyCustomPropertyFromRequest()
        throws IOException
    {
        logTestStart();

        Commandline cli = new Commandline();

        Properties properties = new Properties();
        properties.setProperty( "key", "value" );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setProperties( new DefaultInvocationRequest().setProperties( properties ), cli );

        assertArgumentsPresent( Collections.singleton( "-Dkey=value" ), cli );
    }

    public void testShouldSpecifySingleGoalFromRequest()
        throws IOException
    {
        logTestStart();

        Commandline cli = new Commandline();

        List goals = new ArrayList();
        goals.add( "test" );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setGoals( new DefaultInvocationRequest().setGoals( goals ), cli );

        assertArgumentsPresent( Collections.singleton( "test" ), cli );
    }

    public void testShouldSpecifyTwoGoalsFromRequest()
        throws IOException
    {
        logTestStart();

        Commandline cli = new Commandline();

        List goals = new ArrayList();
        goals.add( "test" );
        goals.add( "clean" );

        TestCommandLineBuilder tcb = new TestCommandLineBuilder();
        tcb.setGoals( new DefaultInvocationRequest().setGoals( goals ), cli );

        assertArgumentsPresent( new HashSet( goals ), cli );
        assertArgumentsPresentInOrder( goals, cli );
    }

    public void testBuildTypicalMavenInvocationEndToEnd()
        throws Exception
    {
        logTestStart();
        File mavenDir = setupTempMavenHomeIfMissing();

        InvocationRequest request = new DefaultInvocationRequest();

        File tmpDir = getTempDir();
        File projectDir = new File( tmpDir, "invoker-tests/typical-end-to-end-cli-build" );

        projectDir.mkdirs();
        toDelete.add( projectDir.getParentFile() );

        request.setBaseDirectory( projectDir );

        Set expectedArgs = new HashSet();
        Set bannedArgs = new HashSet();

        createDummyFile( projectDir, "pom.xml" );

        bannedArgs.add( "-f" );
        bannedArgs.add( "pom.xml" );

        Properties properties = new Properties();
        // this is REALLY bad practice, but since it's just a test...
        properties.setProperty( "maven.tests.skip", "true" );

        expectedArgs.add( "-Dmaven.tests.skip=true" );

        request.setProperties( properties );

        request.setOffline( true );

        expectedArgs.add( "-o" );

        List goals = new ArrayList();

        goals.add( "post-clean" );
        goals.add( "deploy" );
        goals.add( "site-deploy" );
        
        request.setGoals( goals );

        MavenCommandLineBuilder commandLineBuilder = new MavenCommandLineBuilder();

        Commandline commandline = commandLineBuilder.build( request );

        assertArgumentsPresent( expectedArgs, commandline );
        assertArgumentsNotPresent( bannedArgs, commandline );
        assertArgumentsPresentInOrder( goals, commandline );
        
        File mavenFile;
        if ( Os.isFamily( "windows" ) )
        {
            mavenFile = new File( mavenDir, "bin/mvn.bat" );
        }
        else
        {
            mavenFile = new File( mavenDir, "bin/mvn" );
        }
        
        String executable = commandline.getExecutable();
        System.out.println( "Executable is: " + executable );
        
        assertTrue( executable.indexOf( mavenFile.getCanonicalPath() ) > -1 );
        assertEquals( projectDir.getCanonicalPath(), commandline.getWorkingDirectory().getCanonicalPath() );
    }


    public void setUp()
    {
        sysProps = System.getProperties();        

        Properties p = new Properties( sysProps );

        System.setProperties( p );
    }

    public void tearDown()
        throws IOException
    {
        System.setProperties( sysProps );

        for ( Iterator it = toDelete.iterator(); it.hasNext(); )
        {
            File file = (File) it.next();

            if ( file.exists() )
            {
                if ( file.isDirectory() )
                {
                    FileUtils.deleteDirectory( file );
                }
                else
                {
                    file.delete();
                }
            }
        }
    }

    // this is just a debugging helper for separating unit test output...
    private void logTestStart()
    {
//        NullPointerException npe = new NullPointerException();
//        StackTraceElement element = npe.getStackTrace()[1];
//
//        System.out.println( "Starting: " + element.getMethodName() );
    }

    private void assertArgumentsPresentInOrder( List expected, Commandline cli )
    {
        String[] arguments = cli.getArguments();

        int expectedCounter = 0;
        
//        System.out.println( "Command-line has " + arguments.length + " arguments." );

        for ( int i = 0; i < arguments.length; i++ )
        {
//            System.out.println( "Checking argument: " + arguments[i] + " against: " + expected.get( expectedCounter ) );
            
            if ( arguments[i].equals( expected.get( expectedCounter ) ) )
            {
                expectedCounter++;
            }
        }

        assertEquals( "Arguments: " + expected + " were not found or are in the wrong order.", expected.size(),
                      expectedCounter );
    }

    private void assertArgumentsPresent( Set requiredArgs, Commandline cli )
    {
        String[] argv = cli.getArguments();
        List args = Arrays.asList( argv );

        //        System.out.println( "Command-line: " + cli );
        //        System.out.println( "Arguments: " + args );

        for ( Iterator it = requiredArgs.iterator(); it.hasNext(); )
        {
            String arg = (String) it.next();

            assertTrue( "Command-line argument: \'" + arg + "\' is missing.", args.contains( arg ) );
        }
    }

    private void assertArgumentsNotPresent( Set bannedArgs, Commandline cli )
    {
        String[] argv = cli.getArguments();
        List args = Arrays.asList( argv );

        for ( Iterator it = bannedArgs.iterator(); it.hasNext(); )
        {
            String arg = (String) it.next();

            assertFalse( "Command-line argument: \'" + arg + "\' should not be present.", args.contains( arg ) );
        }
    }

    private File createDummyFile( File directory, String filename )
        throws IOException
    {
        File dummyFile = new File( directory, filename );

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( dummyFile );
            writer.write( "This is a dummy file." );
        }
        finally
        {
            IOUtil.close( writer );
        }

        toDelete.add( dummyFile );

        return dummyFile;
    }

    private static final class TestCommandLineBuilder
        extends MavenCommandLineBuilder
    {
        public void checkRequiredState()
        {
            super.checkRequiredState();
        }

        public File findMavenExecutable()
            throws CommandLineConfigurationException
        {
            return super.findMavenExecutable();
        }

        public void setEnvironmentPaths( InvocationRequest request, Commandline cli )
        {
            super.setEnvironmentPaths( request, cli );
        }

        public void setFlags( InvocationRequest request, Commandline cli )
        {
            super.setFlags( request, cli );
        }

        public void setGoals( InvocationRequest request, Commandline cli )
        {
            super.setGoals( request, cli );
        }

        public void setPomLocation( InvocationRequest request, Commandline cli )
        {
            super.setPomLocation( request, cli );
        }

        public void setProperties( InvocationRequest request, Commandline cli )
        {
            super.setProperties( request, cli );
        }

        public void setReactorBehavior( InvocationRequest request, Commandline cli )
        {
            super.setReactorBehavior( request, cli );
        }

        public void setSettingsLocation( InvocationRequest request, Commandline cli )
        {
            super.setSettingsLocation( request, cli );
        }

        public void setShellEnvironment( InvocationRequest request, Commandline cli )
            throws CommandLineConfigurationException
        {
            super.setShellEnvironment( request, cli );
        }

    }

    private File getTempDir()
        throws Exception
    {
        return new File( System.getProperty( "java.io.tmpdir" ) ).getCanonicalFile();
    }
}
