package com.github.safetestspace.testspace.rust;

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.components.ServiceDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.testFramework.TestApplicationManager;

public final class RustTestEnvironment {
    private RustTestEnvironment() {
    }

    public static void prepare() {
        TestApplicationManager.getInstance();
        var rust = (IdeaPluginDescriptorImpl) PluginManagerCore.getPlugin(PluginId.getId("com.jetbrains.rust"));
        if (rust == null) return;
        // RUST-16312: the Rust archive references test services it does not ship.
        rust.projectContainerDescriptor.getServices().replaceAll(RustTestEnvironment::productionService);
        rust.appContainerDescriptor.getServices().replaceAll(RustTestEnvironment::productionService);
    }

    private static ServiceDescriptor productionService(ServiceDescriptor service) {
        if (!"org.rust.cargo.project.model.impl.TestCargoProjectsServiceImpl".equals(service.testServiceImplementation)
                && !"org.rust.test.util.TestUnitTestUtilService".equals(service.testServiceImplementation))
            return service;
        return new ServiceDescriptor(
                service.serviceInterface, service.serviceImplementation, null, service.headlessImplementation,
                service.overrides, service.configurationSchemaKey, service.preload, service.client, service.os);
    }
}
