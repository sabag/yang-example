package com.example;

/*
 * Copyright (c) 2018 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Stopwatch;
import io.lighty.applications.util.ModulesConfig;
import io.lighty.core.common.exceptions.ModuleStartupException;
import io.lighty.core.common.models.YangModuleUtils;
import io.lighty.core.controller.api.LightyController;
import io.lighty.core.controller.api.LightyModule;
import io.lighty.core.controller.impl.LightyControllerBuilder;
import io.lighty.core.controller.impl.config.ConfigurationException;
import io.lighty.core.controller.impl.config.ControllerConfiguration;
import io.lighty.core.controller.impl.util.ControllerConfigUtils;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConf;
import io.lighty.modules.northbound.restconf.community.impl.CommunityRestConfBuilder;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.server.LightyServerBuilder;
import io.lighty.swagger.SwaggerLighty;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {

	public static final Set<YangModuleInfo> ACCEDIAN_MODELS = Set.of(
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.extensions.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.endpoint.ne.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.endpoint.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.endpoint.type.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.l3vpn.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.type.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.session.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.session.twamp.light.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.session.type.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.types.rev221025.$YangModuleInfoImpl.getInstance(),
		org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.$YangModuleInfoImpl.getInstance()

		//        org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.keystore.rev171017.$YangModuleInfoImpl.getInstance(),
		//        org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev221225.$YangModuleInfoImpl.getInstance()
		//        org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.optional.rev221225.$YangModuleInfoImpl.getInstance(),
		//        org.opendaylight.yang.gen.v1.urn.opendaylight.yang.extension.yang.ext.rev130709.$YangModuleInfoImpl.getInstance(),
		//        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.$YangModuleInfoImpl.getInstance(),
		//        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.$YangModuleInfoImpl.getInstance(),
		//        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.library.rev190104.$YangModuleInfoImpl.getInstance()

	);


	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	private LightyController lightyController;
	private SwaggerLighty swagger;
	private CommunityRestConf restconf;
	//    private LightyModule netconfSBPlugin;
	private ModulesConfig modulesConfig = ModulesConfig.getDefaultModulesConfig();

	public static void main(final String[] args) {
		Main app = new Main();
		app.start(args, true);
	}

	public void start() {
		start(new String[] {}, false);
	}

	public void start(final String[] args, final boolean registerShutdownHook) {
		try {
			final ControllerConfiguration singleNodeConfiguration;
			final RestConfConfiguration restconfConfiguration;
/*
                Path configPath = Paths.get("src/main/resources/lighty-config.json");
                LOG.info("using configuration from file {} ...", configPath);
                //1. get controller configuration
                singleNodeConfiguration = ControllerConfigUtils.getConfiguration(Files.newInputStream(configPath));
                //2. get RESTCONF NBP configuration
                restconfConfiguration = RestConfConfigUtils.getRestConfConfiguration(Files.newInputStream(configPath));
                //4. Load modules app configuration
                modulesConfig = ModulesConfig.getModulesConfig(Files.newInputStream(configPath));
*/

			LOG.info("using default configuration ...");
			Set<YangModuleInfo> modelPaths = new HashSet<>(ACCEDIAN_MODELS);

			ArrayNode arrayNode = YangModuleUtils.generateJSONModelSetConfiguration(modelPaths);
			//0. print the list of schema context models
			LOG.info("JSON model config snippet: {}", arrayNode.toString());
			//1. get controller configuration
			singleNodeConfiguration = ControllerConfigUtils.getDefaultSingleNodeConfiguration(modelPaths);

//			singleNodeConfiguration.getActorSystemConfig().setAkkaConfigPath("singlenode/akka-default.conf");
//			singleNodeConfiguration.getActorSystemConfig().setFactoryAkkaConfigPath("singlenode/factory-akka-default.conf");
			singleNodeConfiguration.setModulesConfig("singlenode/modules.conf");
			singleNodeConfiguration.setModuleShardsConfig("singlenode/module-shards.conf");

			//2. get RESTCONF NBP configuration
			restconfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
			restconfConfiguration.setRestconfServletContextPath("/restconf");

			if (registerShutdownHook) {
				Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
			}
			startLighty(singleNodeConfiguration, restconfConfiguration);

		} catch (Exception e) {
			LOG.error("Main RESTCONF-NETCONF application exception: ", e);
			shutdown();
		}
	}

	private void startLighty(final ControllerConfiguration controllerConfiguration,
		final RestConfConfiguration restconfConfiguration
	)
		throws ConfigurationException, ExecutionException, InterruptedException, TimeoutException,
		ModuleStartupException {

		//1. initialize and start Lighty controller (MD-SAL, Controller, YangTools, Akka)
		LightyControllerBuilder lightyControllerBuilder = new LightyControllerBuilder();
		this.lightyController = lightyControllerBuilder.from(controllerConfiguration).build();
		final boolean controllerStartOk = this.lightyController.start()
			.get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
		if (!controllerStartOk) {
			throw new ModuleStartupException("Lighty.io Controller startup failed!");
		}

		//2. build RestConf server
		LightyServerBuilder jettyServerBuilder = new LightyServerBuilder(new InetSocketAddress(
			restconfConfiguration.getInetAddress(), restconfConfiguration.getHttpPort()));
		this.restconf = CommunityRestConfBuilder
			.from(RestConfConfigUtils.getRestConfConfiguration(restconfConfiguration,
				this.lightyController.getServices()))
			.withLightyServer(jettyServerBuilder)
			.build();

		//3. start swagger and RestConf server
		this.swagger = new SwaggerLighty(restconfConfiguration, jettyServerBuilder, this.lightyController.getServices());
		final boolean swaggerStartOk = this.swagger.start()
			.get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
		if (!swaggerStartOk) {
			throw new ModuleStartupException("Lighty.io Swagger startup failed!");
		}
		final boolean restconfStartOk = this.restconf.start()
			.get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
		if (!restconfStartOk) {
			throw new ModuleStartupException("Community Restconf startup failed!");
		}
		this.restconf.startServer();

		//4. start NetConf SBP
		//        netconfSBPConfiguration = NetconfConfigUtils.injectServicesToTopologyConfig(
		//                netconfSBPConfiguration, this.lightyController.getServices());
		//        this.netconfSBPlugin = NetconfTopologyPluginBuilder
		//                .from(netconfSBPConfiguration, this.lightyController.getServices())
		//                .build();
		//        final boolean netconfSBPStartOk = this.netconfSBPlugin.start()
		//                .get(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
		//        if (!netconfSBPStartOk) {
		//            throw new ModuleStartupException("NetconfSB plugin startup failed!");
		//        }
	}

	private void closeLightyModule(final LightyModule module) {
		if (module != null) {
			module.shutdown(modulesConfig.getModuleTimeoutSeconds(), TimeUnit.SECONDS);
		}
	}

	public void shutdown() {
		LOG.info("Lighty.io and RESTCONF-NETCONF shutting down ...");
		final Stopwatch stopwatch = Stopwatch.createStarted();
		//        closeLightyModule(this.netconfSBPlugin);
		closeLightyModule(this.restconf);
		closeLightyModule(this.swagger);
		closeLightyModule(this.lightyController);
		LOG.info("Lighty.io and RESTCONF-NETCONF stopped in {}", stopwatch.stop());
	}

}
