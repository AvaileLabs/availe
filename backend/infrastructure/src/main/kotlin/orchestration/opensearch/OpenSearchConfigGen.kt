package com.availelabs.orchestration.opensearch

import com.availelabs.availe.configuration.SharedOpenSearchConstants
import org.bouncycastle.crypto.generators.BCrypt
import org.slf4j.LoggerFactory
import java.io.File
import java.security.SecureRandom

internal object OpenSearchConfigGen {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Suppress("SetWorldReadable")
    fun generateConfigurationFiles() {
        val projectRootDirectory = System.getProperty("user.dir")
        val generatedConfigDirectory = File(projectRootDirectory, "build/generated/opensearch")
        val securityConfigDirectory = File(generatedConfigDirectory, "opensearch-security")

        if (!generatedConfigDirectory.exists()) {
            generatedConfigDirectory.mkdirs()
        }
        if (!securityConfigDirectory.exists()) {
            securityConfigDirectory.mkdirs()
        }

        generatedConfigDirectory.setReadable(true, false)
        securityConfigDirectory.setReadable(true, false)

        val openSearchConfig = loadOpenSearchConfig()

        logger.info("Generating OpenSearch YAML configuration...")
        generateOpenSearchYml(generatedConfigDirectory)

        logger.info("Generating OpenSearch Dashboards YAML configuration...")
        generateDashboardsYml(generatedConfigDirectory)

        logger.info("Generating OpenSearch Security Configuration (internal_users, roles, etc)...")
        generateSecurityConfig(securityConfigDirectory, openSearchConfig)

        logger.info("OpenSearch configuration files generated at ${generatedConfigDirectory.absolutePath}")
    }

    @Suppress("SetWorldReadable")
    private fun generateOpenSearchYml(directory: File) {
        val content = """
            cluster.name: availe-opensearch-cluster
            network.host: 0.0.0.0

            plugins.security.ssl.transport.pemcert_filepath: /usr/share/opensearch/config/certs/node.pem
            plugins.security.ssl.transport.pemkey_filepath: /usr/share/opensearch/config/certs/node-key.pem
            plugins.security.ssl.transport.pemtrustedcas_filepath: /usr/share/opensearch/config/certs/root-ca.pem
            plugins.security.ssl.transport.enforce_hostname_verification: false

            plugins.security.ssl.http.enabled: true
            plugins.security.ssl.http.pemcert_filepath: /usr/share/opensearch/config/certs/node.pem
            plugins.security.ssl.http.pemkey_filepath: /usr/share/opensearch/config/certs/node-key.pem
            plugins.security.ssl.http.pemtrustedcas_filepath: /usr/share/opensearch/config/certs/root-ca.pem

            plugins.security.allow_default_init_securityindex: true
            plugins.security.authcz.admin_dn:
              - CN=admin

            plugins.security.nodes_dn:
              - CN=${SharedOpenSearchConstants.NODE_1_NAME}
              - CN=${SharedOpenSearchConstants.NODE_2_NAME}

            plugins.security.audit.type: internal_opensearch
            plugins.security.enable_snapshot_restore_privilege: true
            plugins.security.check_snapshot_restore_write_privileges: true
            plugins.security.restapi.roles_enabled: ["all_access", "security_rest_api_access"]
            plugins.security.system_indices.enabled: true
            plugins.security.system_indices.indices: [".opendistro-alerting-config", ".opendistro-alerting-alert*", ".opendistro-anomaly-results*", ".opendistro-anomaly-detector*", ".opendistro-anomaly-checkpoints", ".opendistro-anomaly-detection-state", ".opendistro-reports-*", ".opendistro-notifications-*", ".opendistro-notebooks", ".opendistro-asynchronous-search-response*"]

            cluster.routing.allocation.disk.threshold_enabled: false
        """.trimIndent()

        val file = File(directory, "opensearch.yml")
        file.writeText(content)
        file.setReadable(true, false)
    }

    @Suppress("SetWorldReadable")
    private fun generateDashboardsYml(directory: File) {
        val content = """
            server.name: ${SharedOpenSearchConstants.DASHBOARDS_NAME}
            server.host: "0.0.0.0"
            opensearch.hosts: ["https://${SharedOpenSearchConstants.NODE_1_NAME}:${SharedOpenSearchConstants.OPENSEARCH_PORT}", "https://${SharedOpenSearchConstants.NODE_2_NAME}:${SharedOpenSearchConstants.OPENSEARCH_PORT}"]
            opensearch.ssl.verificationMode: none
            opensearch.username: kibanaserver
            opensearch.password: kibanaserver
            opensearch.requestHeadersWhitelist: ["securitytenant","Authorization"]
            opensearch_security.multitenancy.enabled: true
            opensearch_security.multitenancy.tenants.preferred: ["Private", "Global"]
            opensearch_security.readonly_mode.roles: ["kibana_read_only"]
            opensearch_security.cookie.secure: false
        """.trimIndent()

        val file = File(directory, "opensearch_dashboards.yml")
        file.writeText(content)
        file.setReadable(true, false)
    }

    private fun generateSecurityConfig(directory: File, config: OpenSearchConfig) {
        // 1. internal_users.yml
        val adminHash = hashPassword(config.adminPassword)
        val kibanaHash = hashPassword("kibanaserver")

        val internalUsersContent = """
            _meta:
              type: "internalusers"
              config_version: 2

            admin:
              hash: "$adminHash"
              reserved: true
              backend_roles:
                - "admin"
              description: "Admin user"

            kibanaserver:
              hash: "$kibanaHash"
              reserved: true
              description: "Kibana server user"
        """.trimIndent()
        writeSecurityFile(directory, "internal_users.yml", internalUsersContent)

        // 2. roles_mapping.yml
        val rolesMappingContent = """
            _meta:
              type: "rolesmapping"
              config_version: 2

            all_access:
              reserved: false
              backend_roles:
                - "admin"
              description: "Maps admin to all_access"

            kibana_server:
              reserved: true
              users:
                - "kibanaserver"
        """.trimIndent()
        writeSecurityFile(directory, "roles_mapping.yml", rolesMappingContent)

        // 3. config.yml (Security Main Config)
        val securityConfigContent = """
            _meta:
              type: "config"
              config_version: 2

            config:
              dynamic:
                http:
                  anonymous_auth_enabled: false
                  xff:
                    enabled: false
                authc:
                  basic_internal_auth_domain:
                    description: "Authenticate via HTTP Basic against internal users database"
                    http_enabled: true
                    transport_enabled: true
                    order: 1
                    http_authenticator:
                      type: basic
                      challenge: true
                    authentication_backend:
                      type: intern
        """.trimIndent()
        writeSecurityFile(directory, "config.yml", securityConfigContent)

        // 4. roles.yml (Empty default)
        val rolesContent = """
            _meta:
              type: "roles"
              config_version: 2
        """.trimIndent()
        writeSecurityFile(directory, "roles.yml", rolesContent)

        // 5. tenants.yml (Empty default)
        val tenantsContent = """
            _meta:
              type: "tenants"
              config_version: 2
        """.trimIndent()
        writeSecurityFile(directory, "tenants.yml", tenantsContent)

        // 6. nodes_dn.yml (Empty default)
        val nodesDnContent = """
            _meta:
              type: "nodesdn"
              config_version: 2
        """.trimIndent()
        writeSecurityFile(directory, "nodes_dn.yml", nodesDnContent)

        // 7. whitelist.yml (Empty default)
        val whitelistContent = """
            _meta:
              type: "whitelist"
              config_version: 2
        """.trimIndent()
        writeSecurityFile(directory, "whitelist.yml", whitelistContent)

        // 8. audit.yml (Empty default)
        val auditContent = """
            _meta:
              type: "audit"
              config_version: 2
        """.trimIndent()
        writeSecurityFile(directory, "audit.yml", auditContent)
    }

    @Suppress("SetWorldReadable")
    private fun writeSecurityFile(directory: File, filename: String, content: String) {
        val file = File(directory, filename)
        file.writeText(content)
        file.setReadable(true, false)
    }

    // Standard OpenBSD BCrypt Format Implementation
    private fun hashPassword(password: String): String {
        val cost = 12
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        // Bouncy Castle's generate returns the raw hash bytes (P and S boxes), not the formatted string
        val passwordBytes = BCrypt.passwordToByteArray(password.toCharArray())
        val rawHash = BCrypt.generate(passwordBytes, salt, cost)

        // We must manually construct the Modular Crypt Format string: $2a$12$salt...hash...
        // Format: $2a$ + cost(2 digits) + $ + base64(salt) + base64(hash)
        // Note: BCrypt uses a non-standard Base64 alphabet.

        val stringBuilder = StringBuilder()
        stringBuilder.append($$"$2a$")
        stringBuilder.append(cost)
        stringBuilder.append("$")

        stringBuilder.append(encodeBase64(salt, 16))
        stringBuilder.append(encodeBase64(rawHash, 23)) // 23 bytes of hash used for the string

        return stringBuilder.toString()
    }

    // Custom Base64 encoder for BCrypt (OpenBSD / Radix-64)
    // Alphabet: ./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789
    private fun encodeBase64(data: ByteArray, length: Int): String {
        val dictionary = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val stringBuilder = StringBuilder()
        var off = 0
        var len = length

        if (len <= 0) return ""
        if (len > data.size) len = data.size

        var c1: Int
        var c2: Int

        while (off < len) {
            c1 = data[off++].toInt() and 0xff
            stringBuilder.append(dictionary[(c1 shr 2) and 0x3f])
            c1 = (c1 and 0x03) shl 4
            if (off >= len) {
                stringBuilder.append(dictionary[c1 and 0x3f])
                break
            }
            c2 = data[off++].toInt() and 0xff
            c1 = c1 or ((c2 shr 4) and 0x0f)
            stringBuilder.append(dictionary[c1 and 0x3f])
            c1 = (c2 and 0x0f) shl 2
            if (off >= len) {
                stringBuilder.append(dictionary[c1 and 0x3f])
                break
            }
            c2 = data[off++].toInt() and 0xff
            c1 = c1 or ((c2 shr 6) and 0x03)
            stringBuilder.append(dictionary[c1 and 0x3f])
            stringBuilder.append(dictionary[c2 and 0x3f])
        }
        return stringBuilder.toString()
    }
}