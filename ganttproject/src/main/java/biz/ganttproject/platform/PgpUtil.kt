/*
Copyright 2020 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package biz.ganttproject.platform

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import net.sourceforge.ganttproject.GPLogger
import org.bouncycastle.openpgp.*
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider
import java.io.*
import java.util.*
import org.eclipse.core.runtime.Platform as Eclipsito

/**
 * Utilities for the verification of artifacts downloaded by the update system.
 *
 * The main entrance point for the updater is verifyFile. It expects the public key, and if the public key
 * is not provided, it is searched in the root classpath resources as /ganttproject-3.0.pub.asc.
 * The default location may be overridden with the system property eclipsito.update.public_key
 *
 * @author dbarashev (Dmitry Barashev)
 */
object PgpUtil {
  private val LOG = GPLogger.create("App.Update.Pgp")
  private val FP_CALC: KeyFingerPrintCalculator = BcKeyFingerprintCalculator()

  private val ourPublicKey by lazy {
    val publicKeyResource = System.getProperty("app.update.public_key", "/ganttproject-3.0.pub.asc")
    val publicKeyStream = Eclipsito::class.java.getResourceAsStream(publicKeyResource) ?:
      throw RuntimeException(
        "Failed to read the public key from $publicKeyResource. This resource is missing.".also { LOG.error(it) }
      )
    try {
      readPublicKey(publicKeyStream)?.also {
        LOG.debug("Update system will use a public key {}", it.publicKeyToString())
      } ?: throw RuntimeException("Failed to read $publicKeyResource as a PGP public key.")
    } catch (e: IOException) {
      throw RuntimeException("Failed to read $publicKeyResource as a PGP public key.", e)
    } catch (e: PGPException) {
      throw RuntimeException("Failed to read $publicKeyResource as a PGP public key.", e)
    }
  }

  @Throws(IOException::class, PGPException::class)
  private fun readPublicKey(keyStream: InputStream): PGPPublicKey? =
    PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyStream), FP_CALC)
      .keyRings.asSequence().flatMap { it.publicKeys.asSequence() }
      .firstOrNull{ it.isEncryptionKey }


  private fun PGPPublicKey.publicKeyToString(): String {
    val info = publicKeyInfo(this)
    return String.format("%n%s%n%s", info.uid, info.keyFingerprint)
  }

  private fun publicKeyInfo(publicKey: PGPPublicKey): PgpInfo {
    val ids = ArrayList<String>()
    publicKey.userIDs.forEachRemaining { e: String -> ids.add(e) }
    val fingerprintString = StringBuilder()
    for (b in publicKey.fingerprint) {
      fingerprintString.append(String.format("%02X", b))
    }
    return PgpInfo(java.lang.String.join("\n", ids), fingerprintString.toString(), null)
  }

  private fun <T> find(pgpObjects: List<Any>, clazz: Class<T>): T? {
    for (o in pgpObjects) {
      if (o.javaClass.isAssignableFrom(clazz)) {
        return o as T
      }
    }
    return null
  }

  @Throws(Exception::class)
  fun getSignature(publicKey: PGPPublicKey, sigStream: InputStream): PGPSignature {
    try {
      PGPUtil.getDecoderStream(sigStream).use { pgpStream ->
        val pgpObjects = JcaPGPObjectFactory(pgpStream).toList()
        val sigList = find(pgpObjects, PGPSignatureList::class.java)
          ?: throw RuntimeException("Failed to verify PGP signature: siglist not found")
        val signature = sigList[0]
        signature.init(JcaPGPContentVerifierBuilderProvider(), publicKey)
        return signature
      }
    } catch (e: Exception) {
      val msg = "Failed to read the signature."
      LOG.error(msg, e)
      throw Exception(msg, e)
    } catch (e: PGPException) {
      val msg = "Failed to read the signature."
      LOG.error(msg, e)
      throw Exception(msg, e)
    } catch (e: IOException) {
      val msg = "Failed to read the signature."
      LOG.error(msg, e)
      throw Exception(msg, e)
    }
  }

  /**
   * Verifies data file PGP signature.
   *
   * @param dataFile Data file to be verified
   * @param signatureStream The contents of the signature
   * @param publicKey public key for the verification or null if the default public key shall be used
   * @return PgpInfo instance if verification was successfull. Throws PgpUtil.Exception otherwise.
   * @throws PgpUtil.Exception in case of internal errors or verification failure
   */
  @Throws(Exception::class)
  fun verifyFile(dataFile: File, signatureStream: InputStream, publicKey: PGPPublicKey = ourPublicKey): PgpInfo {

    return try {
      val signature = getSignature(publicKey, BufferedInputStream(signatureStream))
      signature.update(FileInputStream(dataFile).readAllBytes())
      if (signature.verify() && signature.keyID == publicKey.keyID) {
        val keyInfo = publicKeyInfo(publicKey)
        PgpInfo(keyInfo.uid, keyInfo.keyFingerprint, signature.creationTime)
      } else {
        val msg = "Verification failed because the signature and the public key do no match."
        throw Exception(msg)
      }
    } catch (e: IOException) {
      val msg = "Verification failed because of the internal error."
      LOG.error(msg, e)
      throw Exception(msg, e)
    } catch (e: PGPException) {
      val msg = "Verification failed because of the internal error."
      LOG.error(msg, e)
      throw Exception(msg, e)
    }
  }

  /**
   * Utility main function for quick verifications.
   */
  @Throws(IOException::class, PGPException::class)
  @JvmStatic
  fun main(argv: Array<String>) {
    val args = Args()
    JCommander.newBuilder().addObject(args).build().parse(*argv)

    val pgpPublicKey = readPublicKey(BufferedInputStream(FileInputStream(args.publicKey)))
      ?: throw RuntimeException("Cannot read a public key from ${args.publicKey}")
    val pgpInfo = verifyFile(File(args.inputFile), FileInputStream(args.sigFile), pgpPublicKey)
    println("""
      OK
      Signed by: ${pgpInfo.uid}
      Signed at: ${pgpInfo.signatureCreationTime}
      Fingerprint: ${pgpInfo.keyFingerprint}
    """.trimIndent()
    )
  }

  class PgpInfo(val uid: String, val keyFingerprint: String, val signatureCreationTime: Date?)
  internal class Args {
    @Parameter(names = ["--key"], description = "Public key file")
    var publicKey = ""

    @Parameter(names = ["--data"], description = "Input file")
    var inputFile = ""

    @Parameter(names = ["--sig"], description = "Signature file")
    var sigFile = ""
  }

  internal class Exception : RuntimeException {
    constructor(msg: String?, cause: Throwable?) : super(msg, cause) {}
    constructor(msg: String?) : super(msg) {}
  }
}
