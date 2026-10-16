package com.needlos.empleados;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra las properties de la feature de empleados (se mantiene dentro del propio paquete). */
@Configuration
@EnableConfigurationProperties(EmpleadoProperties.class)
public class EmpleadoConfig {}
