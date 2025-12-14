package com.availelabs.utils

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

internal object SudoExecutor {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun executePrivilegedCommand(password: String, command: String) {
        logger.info("Executing privileged command: {}", command)

        val processBuilder = ProcessBuilder(
            "/bin/sh",
            "-c",
            "echo '$password' | sudo -S $command"
        )

        val process = processBuilder.start()

        val outputReader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))

        val outputBuilder = StringBuilder()
        val errorBuilder = StringBuilder()

        val outputThread = Thread {
            outputReader.lines().forEach { line ->
                outputBuilder.append(line).append("\n")
            }
        }

        val errorThread = Thread {
            errorReader.lines().forEach { line ->
                errorBuilder.append(line).append("\n")
            }
        }

        outputThread.start()
        errorThread.start()

        val finished = process.waitFor(30, TimeUnit.SECONDS)
        outputThread.join()
        errorThread.join()

        if (!finished) {
            process.destroyForcibly()
            throw IllegalStateException("Privileged command timed out: $command")
        }

        if (process.exitValue() != 0) {
            val combinedOutput = "STDOUT:\n$outputBuilder\nSTDERR:\n$errorBuilder"
            throw IllegalStateException("Privileged command failed with exit code ${process.exitValue()}.\nOutput: $combinedOutput")
        }
    }
}