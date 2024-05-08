package controlador;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.examples.signature.SigUtils;
import org.apache.pdfbox.examples.signature.ValidationTimeStamp;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.pqc.crypto.crystals.dilithium.*;
import org.bouncycastle.pqc.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.pqc.crypto.util.PublicKeyFactory;
import org.bouncycastle.pqc.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;
import org.bouncycastle.cert.jcajce.JcaCertStore;

/*
 * La clase Controlador será la encargada de conectar los métodos de Dilithium con la aplicación Notario Digital
 */
public class FirmaDigital {

	private DilithiumKeyGenerationParameters generadorClaves;
	private static DilithiumPrivateKeyParameters clavePrivada;
	private static DilithiumPublicKeyParameters clavePublica;
	private static AsymmetricCipherKeyPair parClaves;
	private DilithiumKeyParameters params;
	private static int dilithiumMode;
	private static byte[] mensaje, firma;
	private static SignatureOptions signatureOptions;
	private static PDVisibleSignDesigner visibleSignDesigner;
	private final static PDVisibleSigProperties visibleSignatureProperties = new PDVisibleSigProperties();
	private static PDDocument doc;
	private static PrivateKey sk;
	private static PublicKey pk;
	private static X509Certificate certificado;
	private final static String dir = System.getProperty("user.dir");

	/**
	 * Para crear el objeto, se le pasa el archivo de PDF ¡¡¡¡REVISAR!!!! y el nivel
	 * de seguridad de Dilithium para iniciar los parámetros
	 * 
	 * @param archivo_pdf
	 * @param dilithiumMode
	 */
	public FirmaDigital(PDDocument documento, int dilithiumMode) {

		this.doc = documento;
		this.dilithiumMode = dilithiumMode;
		if (dilithiumMode == 3) {
			this.generadorClaves = new DilithiumKeyGenerationParameters(new SecureRandom(),
					DilithiumParameters.dilithium3);
		} else if (dilithiumMode == 5) {
			this.generadorClaves = new DilithiumKeyGenerationParameters(new SecureRandom(),
					DilithiumParameters.dilithium5);
		} else {
			dilithiumMode = 2;
			this.generadorClaves = new DilithiumKeyGenerationParameters(new SecureRandom(),
					DilithiumParameters.dilithium2);
		}
		parClaves = generarParClaves();
		mensaje = "¡Dilithium!".getBytes();
		try {
			firmar((DilithiumPrivateKeyParameters) parClaves.getPrivate(), this.mensaje);
		} catch (OperatorCreationException | CertificateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getOS() {
		return System.getProperty("os.name");
	}

	public PDDocument getPDDocument() {
		return this.doc;
	}

	public byte[] getFirma() {
		return firma;
	}

	public X509Certificate getCertificado() {
		return certificado;
	}

	public PrivateKey getPrivateKey() {
		return sk;
	}

	public SignatureOptions getSignatureOptions() {
		return signatureOptions;
	}

	/**
	 * Ejecuta la firma de Dilithium. Después almacena los datos de la firma para
	 * después utilizarlos en la escritura del PDF.
	 * 
	 * @return 0 si todo está correcto. 1 si hay algún error
	 * @throws IOException
	 * @throws CertificateException
	 * @throws OperatorCreationException
	 * @throws CertificateEncodingException
	 */
	public void firmar(DilithiumPrivateKeyParameters privateKey, byte[] message)
			throws CertificateEncodingException, OperatorCreationException, CertificateException, IOException {
		DilithiumSigner signer = new DilithiumSigner();
		if (dilithiumMode == 3) {
			signer.init(true,
					new DilithiumPrivateKeyParameters(DilithiumParameters.dilithium3, privateKey.getRho(),
							privateKey.getK(), privateKey.getTr(), privateKey.getS1(), privateKey.getS2(),
							privateKey.getT0(), privateKey.getT1()));
		} else if (dilithiumMode == 5) {
			signer.init(true,
					new DilithiumPrivateKeyParameters(DilithiumParameters.dilithium5, privateKey.getRho(),
							privateKey.getK(), privateKey.getTr(), privateKey.getS1(), privateKey.getS2(),
							privateKey.getT0(), privateKey.getT1()));
		} else {
			signer.init(true,
					new DilithiumPrivateKeyParameters(DilithiumParameters.dilithium2, privateKey.getRho(),
							privateKey.getK(), privateKey.getTr(), privateKey.getS1(), privateKey.getS2(),
							privateKey.getT0(), privateKey.getT1()));
		}
		this.firma = signer.generateSignature(this.mensaje);
		firmarDocumento();
	}

	public boolean verificar() {
		DilithiumSigner verifier = new DilithiumSigner();
		DilithiumPublicKeyParameters publicKey = (DilithiumPublicKeyParameters) parClaves.getPublic();
		if (dilithiumMode == 3) {
			verifier.init(false,
					new DilithiumPublicKeyParameters(DilithiumParameters.dilithium3, publicKey.getEncoded()));
		} else if (dilithiumMode == 5) {
			verifier.init(false,
					new DilithiumPublicKeyParameters(DilithiumParameters.dilithium5, publicKey.getEncoded()));
		} else {
			verifier.init(false,
					new DilithiumPublicKeyParameters(DilithiumParameters.dilithium2, publicKey.getEncoded()));
		}
		return verifier.verifySignature(this.mensaje, this.firma);
	}

	public static AsymmetricCipherKeyPair generarParClaves() {
		DilithiumKeyPairGenerator generator = new DilithiumKeyPairGenerator();
		generator.init(new DilithiumKeyGenerationParameters(new SecureRandom(), DilithiumParameters.dilithium2));

		AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();

		return keyPair;
	}

	public static void firmarDocumento() throws CertificateEncodingException, OperatorCreationException, CertificateException, IOException {
		try(FileOutputStream archivoOutput = new FileOutputStream(dir + "\\archivo_firmado.pdf")){
			byte[] codigoFirma = codigoFirma();
			// Creamos el elemento visual de firma
			visibleSignDesigner = new PDVisibleSignDesigner(doc, new FileInputStream("C:\\Users\\David\\Pictures\\imagen.png"), 1);
			visibleSignDesigner.xAxis(100).yAxis(100).zoom(0).adjustForRotation();
			// Propiedades de la firma
			visibleSignatureProperties.signerName("David García Diez").signerLocation("Universidad de León")
					.signatureReason("Firma con Dilithium").preferredSize(50).page(1).visualSignEnabled(true)
					.setPdVisibleSignature(visibleSignDesigner);

			int accessPermissions = SigUtils.getMDPPermission(doc);
			if (accessPermissions == 1) {
				throw new IllegalStateException(
						"No changes to the document are permitted due to DocMDP transform parameters dictionary");
			}
			PDSignature signature = new PDSignature();
			if (doc.getVersion() >= 1.5f && accessPermissions == 0) {
				SigUtils.setMDPPermission(doc, signature, 2);
			}
			PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm(null);
			if (acroForm != null && acroForm.getNeedAppearances()) {
				// PDFBOX-3738 NeedAppearances true results in visible signature becoming
				// invisible
				// with Adobe Reader
				if (acroForm.getFields().isEmpty()) {
					// we can safely delete it if there are no fields
					acroForm.getCOSObject().removeItem(COSName.NEED_APPEARANCES);
					// note that if you've set MDP permissions, the removal of this item
					// may result in Adobe Reader claiming that the document has been changed.
					// and/or that field content won't be displayed properly.
					// ==> decide what you prefer and adjust your code accordingly.
				} else {
					System.out.println("/NeedAppearances is set, signature may be ignored by Adobe Reader");
				}
			}

			signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
			signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
			visibleSignatureProperties.buildSignature();
			signature.setName("David García Diez");
			signature.setLocation("Universidad de León");
			signature.setReason("Análisis de Algoritmos Post-Cuánticos");
			signature.setSignDate(Calendar.getInstance());
			SignatureInterface signatureInterface = new SignatureInterface() {

				public byte[] sign(InputStream arg0) throws IOException {
					return firma;
				}
				
			};

			// register signature dictionary and sign interface
			if (visibleSignatureProperties.isVisualSignEnabled()) {
				signatureOptions = new SignatureOptions();
				signatureOptions.setVisualSignature(visibleSignatureProperties.getVisibleSignature());
				signatureOptions.setPage(visibleSignatureProperties.getPage() - 1);
				doc.addSignature(signature, signatureInterface, signatureOptions);
			} else {
				doc.addSignature(signature, signatureInterface);
			}
			ExternalSigningSupport externalSigning = doc.saveIncrementalForExternalSigning(archivoOutput);

			externalSigning.setSignature(codigoFirma);

		}
		
	}

	private static X509Certificate generarCertificado() throws OperatorCreationException, CertificateException,
			IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {

		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		// Generar par de claves para Dilithium
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Dilithium", "BC");
		if (dilithiumMode == 5) {
			keyPairGenerator.initialize(DilithiumParameterSpec.dilithium5);
		} else if (dilithiumMode == 3) {
			keyPairGenerator.initialize(DilithiumParameterSpec.dilithium3);
		} else {
			keyPairGenerator.initialize(DilithiumParameterSpec.dilithium2);
		}
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		pk = keyPair.getPublic();
		sk = keyPair.getPrivate();
		SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(pk.getEncoded());
		// Crear el emisor y el sujeto del certificado
		X500Name issuerName = new X500Name("CN=Issuer");
		X500Name subjectName = new X500Name("CN=Subject");

		// Crear el generador de certificados
		X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuerName,
				BigInteger.valueOf(System.currentTimeMillis()), new Date(System.currentTimeMillis()),
				new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000), // 1 año de validez
				subjectName, publicKeyInfo);

		// Construir el firmante del certificado
		ContentSigner signer = new JcaContentSignerBuilder("Dilithium").setProvider("BC").build(sk);

		// Construir el certificado
		X509CertificateHolder certHolder = certBuilder.build(signer);

		// Convertir el certificado a la clase X509Certificate de Java
		X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);

		// Imprimir el certificado (opcional)
		System.out.println(cert.toString());
		return cert;
	}

	public static byte[] codigoFirma() throws IOException {
		// cannot be done private (interface)
		try {
			CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
			X509Certificate cert = generarCertificado();
			ContentSigner dilithiumSigner;
			if (dilithiumMode == 5) {
				dilithiumSigner = new JcaContentSignerBuilder("Dilithium5").build(sk);
			} else if (dilithiumMode == 3) {
				dilithiumSigner = new JcaContentSignerBuilder("Dilithium3").build(sk);
			} else {
				dilithiumSigner = new JcaContentSignerBuilder("Dilithium2").build(sk);
			}

			gen.addSignerInfoGenerator(
					new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
							.build(dilithiumSigner, cert));
			gen.addCertificates(new JcaCertStore(Arrays.asList(cert)));
			CMSTypedData msg = new CMSProcessableByteArray(mensaje);
			CMSSignedData signedData = gen.generate(msg, false);

			return signedData.getEncoded();
		} catch (GeneralSecurityException | CMSException | OperatorCreationException e) {
			throw new IOException(e);
		}
	}

}
