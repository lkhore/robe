package io.robe.admin;

import com.codahale.metrics.servlets.MetricsServlet;
import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import io.dropwizard.Application;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jetty.NonblockingServletHolder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.dropwizard.views.ViewRenderer;
import io.dropwizard.views.freemarker.FreemarkerViewRenderer;
import io.robe.admin.cli.InitializeCommand;
import io.robe.admin.guice.module.HibernateModule;
import io.robe.admin.hibernate.dao.*;
import io.robe.assets.AdvancedAssetBundle;
import io.robe.auth.token.TokenAuthBundle;
import io.robe.auth.token.TokenAuthenticator;
import io.robe.auth.token.jersey.TokenFactory;
import io.robe.common.exception.ExceptionMapperBinder;
import io.robe.common.exception.RobeExceptionMapper;
import io.robe.common.exception.RobeRuntimeException;
import io.robe.common.service.search.SearchFactoryProvider;
import io.robe.guice.GuiceBundle;
import io.robe.hibernate.RobeHibernateBundle;
import io.robe.mail.MailBundle;
import io.robe.quartz.QuartzBundle;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


/**
 * Default io.robe.admin class of Robe.
 * If you extend this class on your applications io.robe.admin class and call super methods at
 * overridden methods you will still benefit of robe souse.
 */
public class RobeApplication<T extends RobeConfiguration> extends Application<T> {


    private static final Logger LOGGER = LoggerFactory.getLogger(RobeApplication.class);
    private InitializeCommand initCommand;
    private static String configurationPath;

    public static void main(String[] args) throws Exception {

        RobeApplication application = new RobeApplication();
        if (args.length < 2) {
            LOGGER.error("Give a config yml path.");
        } else {
            configurationPath = args[1];
        }
        application.run(args);
    }

    /**
     * Adds
     * Hibernate bundle for PROVIDER connection
     * Asset bundle for io.robe.admin screens and
     * Class scanners for
     * <ul>
     * <li>Entities</li>
     * <li>HealthChecks</li>
     * <li>Providers</li>
     * <li>InjectableProviders</li>
     * <li>Resources</li>
     * <li>Tasks</li>
     * <li>Managed objects</li>
     * </ul>
     *
     * @param bootstrap
     */
    @Override
    public void initialize(Bootstrap<T> bootstrap) {
        T config = loadConfiguration(bootstrap);
        RobeHibernateBundle<T> hibernateBundle = RobeHibernateBundle.createInstance(
                config.getHibernate().getScanPackages(),
                config.getHibernate().getEntities());
        addGuiceBundle(bootstrap, hibernateBundle);
        bootstrap.addBundle(hibernateBundle);
        initCommand = new InitializeCommand(this, hibernateBundle);

        bootstrap.addBundle(new TokenAuthBundle<T>());
        bootstrap.addCommand(initCommand);
        bootstrap.addBundle(new QuartzBundle<T>());
        bootstrap.addBundle(new ViewBundle());
        bootstrap.addBundle(new ViewBundle(ImmutableList.<ViewRenderer>of(new FreemarkerViewRenderer())));
        bootstrap.addBundle(new MailBundle<T>());
        bootstrap.addBundle(new AdvancedAssetBundle<T>());

    }

    /**
     * Implement this method in order to give your interested packages
     *
     * @return
     */
    protected T loadConfiguration(Bootstrap bootstrap) {
        try {
            return
                    (T) bootstrap.getConfigurationFactoryFactory().create(
                            bootstrap.getApplication().getConfigurationClass(),
                            bootstrap.getValidatorFactory().getValidator(),
                            bootstrap.getObjectMapper(), "")
                            .build(new File(configurationPath));
        } catch (Exception e) {
            throw new RobeRuntimeException("Can't load configuration :"+ configurationPath, e);
        }
    }


    private void addGuiceBundle(Bootstrap<T> bootstrap, RobeHibernateBundle hibernateBundle) {
        List<Module> modules = new LinkedList<>();
        modules.add(new HibernateModule(hibernateBundle));

        bootstrap.addBundle(new GuiceBundle<T>(modules, bootstrap.getApplication().getConfigurationClass()));
    }


    /**
     * {@inheritDoc}
     * In addition adds exception mapper.
     *
     * @param configuration
     * @param environment
     * @throws Exception
     */
    @UnitOfWork
    @Override
    public void run(T configuration, Environment environment) throws Exception {
        TokenFactory.authenticator = new TokenAuthenticator(
                GuiceBundle.getInjector().getInstance(UserDao.class),
                GuiceBundle.getInjector().getInstance(ServiceDao.class),
                GuiceBundle.getInjector().getInstance(RoleDao.class),
                GuiceBundle.getInjector().getInstance(PermissionDao.class),
                GuiceBundle.getInjector().getInstance(RoleGroupDao.class));
        TokenFactory.tokenKey = configuration.getAuth().getTokenKey();
        addExceptionMappers(environment);
        /**
         * register {@link SearchFactoryProvider}
         */
        environment.jersey().register(new SearchFactoryProvider.Binder());
        environment.jersey().register(MultiPartFeature.class);

        environment.getApplicationContext().setAttribute(
                MetricsServlet.METRICS_REGISTRY,
                environment.metrics());
        environment.getApplicationContext().addServlet(
                new NonblockingServletHolder(new MetricsServlet()), "/metrics/*");

        if ("TEST".equals(System.getProperty("env"))) {
            getInitCommand().execute(configuration);
        }
    }

    private void addExceptionMappers(Environment environment) {
        environment.jersey().register(RobeExceptionMapper.class);
        environment.jersey().register(new ExceptionMapperBinder(true));
    }

    protected InitializeCommand getInitCommand() {
        return initCommand;
    }
}
