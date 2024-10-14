package com.fis.ekyc.nfc.build_in.eidparser;

import android.text.TextUtils;
import android.util.Log;

import com.fis.ekyc.nfc.build_in.bouncycastle.asn1.ASN1Encodable;
import com.fis.ekyc.nfc.build_in.bouncycastle.asn1.ASN1Integer;
import com.fis.ekyc.nfc.build_in.bouncycastle.asn1.DERSequence;
import com.fis.ekyc.nfc.build_in.eidparser.ext.JMRTDSecurityProvider;
import com.fis.ekyc.nfc.build_in.eidparser.lds.ActiveAuthenticationInfo;
import com.fis.ekyc.nfc.build_in.eidparser.lds.CardAccessFile;
import com.fis.ekyc.nfc.build_in.eidparser.lds.ChipAuthenticationPublicKeyInfo;
import com.fis.ekyc.nfc.build_in.eidparser.lds.PACEInfo;
import com.fis.ekyc.nfc.build_in.eidparser.lds.SODFile;
import com.fis.ekyc.nfc.build_in.eidparser.lds.SecurityInfo;
import com.fis.ekyc.nfc.build_in.eidparser.lds.icao.DG14File;
import com.fis.ekyc.nfc.build_in.eidparser.lds.icao.DG15File;
import com.fis.ekyc.nfc.build_in.eidparser.protocol.AAResult;
import com.fis.ekyc.nfc.build_in.model.CardResult;
import com.fis.ekyc.nfc.build_in.model.CheckingCode;
import com.fis.ekyc.nfc.build_in.model.ResultCode;
import com.fis.ekyc.nfc.build_in.scuba.smartcards.CardFileInputStream;
import com.fis.ekyc.nfc.build_in.scuba.smartcards.CardService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Cipher;

import vn.kalapa.ekyc.utils.Helpers;

public class ICaoReaderParser {
    private static final String TAG = ICaoReaderParser.class.getSimpleName();
    private CardService _card = null;
    private IdCardService _service = null;
    private String _cccdId = "";
    private CardResult _result = null;
    private DG14File _dgSecurityInfoFile = null;
    private DG15File _dgPublicKeyFile = null;
    private SODFile _sodFile = null;

    public ICaoReaderParser() {
    }

    public CardResult readData(CardService card, String cccdId, boolean hashCheck, boolean chipCheck, boolean activeCheck) {
        this._result = new CardResult();
        if (!TextUtils.isEmpty(cccdId) && cccdId.length() == 12) {
            try {
                Log.d(TAG, "Open card service...");
                card.open();
                this._card = card;
            } catch (Exception var10) {
                this._result.setCode(ResultCode.CANNOT_OPEN_DEVICE);
                Log.d(TAG, "Exception: " + var10.getMessage());
                return this._result;
            }

            try {
                Log.d(TAG, "Open passport service...");
                IdCardService service = new IdCardService(card, 256, 223, false, false);
                service.open();
                this._service = service;
            } catch (Exception var9) {
                this._result.setCode(ResultCode.CARD_NOT_FOUND);
                this.closeALl();
                Log.d(TAG, "Exception: " + var9.getMessage());
                return this._result;
            }

            this._cccdId = cccdId;
            ResultCode ecapodCode = this.ecapod();
            this._result.setCode(ecapodCode);
            if (this._result.getCode() != ResultCode.SUCCESS) {
                this.closeALl();
                return this._result;
            } else {
                ResultCode readDgCode = this.spuorGataDllAdaer();
                if (readDgCode != ResultCode.SUCCESS) {
                    return this._result;
                } else {
                    ResultCode actAuthenCode;
                    if (chipCheck) {
                        actAuthenCode = this.chipAuthentication();
                        if (actAuthenCode != ResultCode.SUCCESS) {
                            this._result.setChipCheck(CheckingCode.FAILED);
                        } else {
                            this._result.setChipCheck(CheckingCode.PASS);
                        }
                    }

                    if (activeCheck) {
                        actAuthenCode = this.activeAuthentication();
                        if (actAuthenCode != ResultCode.SUCCESS) {
                            this._result.setActiveCheck(CheckingCode.FAILED);
                        } else {
                            this._result.setActiveCheck(CheckingCode.PASS);
                        }
                    }

                    this.closeALl();
                    return this._result;
                }
            }
        } else {
            this._result.setCode(ResultCode.WRONG_CCCDID);
            return this._result;
        }
    }

    private ResultCode ecapod() {
        try {
            Log.d(TAG, "KLP Start Pace...!");
            Helpers.Companion.printLog(TAG, "KLP Start Pace...!");
            CardAccessFile cardAccessFile = new CardAccessFile(this._service.getInputStream((short) 284));
            Collection<SecurityInfo> securityInfoCollection = cardAccessFile.getSecurityInfos();
            int Startpos = this._cccdId.length() - 6;
            String can = this._cccdId.substring(Startpos).trim();
            PACEKeySpec paceKey = PACEKeySpec.createCANKey(can);
            Iterator var6 = securityInfoCollection.iterator();

            while (var6.hasNext()) {
                SecurityInfo securityInfo = (SecurityInfo) var6.next();
                if (securityInfo instanceof PACEInfo) {
                    PACEInfo paceInfo = (PACEInfo) securityInfo;
                    this._service.doPACE(paceKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()), (BigInteger) null);
                    Log.w(TAG, "ecap successful!");
                    this._service.sendSelectApplet(true);
                    return ResultCode.SUCCESS;
                }
            }
        } catch (Exception var9) {
            Log.w(TAG, var9);
            if (var9.getLocalizedMessage() != null && var9.getLocalizedMessage().contains("authentication token generation step"))
                return ResultCode.WRONG_CCCDID;
            return ResultCode.CARD_LOST_CONNECTION;
        }
        return ResultCode.WRONG_CCCDID;
    }

    private ResultCode spuorGataDllAdaer() {
        try {
            this._result.setCode(ResultCode.SUCCESS);
            CardFileInputStream sodIn = this._service.getInputStream((short) 285);
            this._sodFile = new SODFile(sodIn);
            this._result.setSOD(this._sodFile.getEncoded());
            MessageDigest digest = MessageDigest.getInstance(this._sodFile.getDigestAlgorithm());
            Map<Integer, byte[]> dataHashes = this._sodFile.getDataGroupHashes();
            Iterator var5 = dataHashes.keySet().iterator();

            while (var5.hasNext()) {
                Integer item = (Integer) var5.next();
                if (((byte[]) dataHashes.get(item)).length > 0) {
                    Log.d(TAG, "Found hash of Data Group: " + item + "; ");
                    CardFileInputStream cfInputStream = null;
                    byte[] dgHashValue = null;

                    try {
                        if (item != 3) {
                            Log.w(TAG, "Ignore DataGroup 3 (Is FingerPrint - cannot read)!");
                            cfInputStream = this._service.getInputStream((short) (257 + item - 1));
                        }
                    } catch (Exception var9) {
                        Log.e(TAG, "Error", var9);
                    }

                    if (cfInputStream == null) {
                        Log.w(TAG, String.format("Cannot read Data Groupd: %d, ignore!", item));
                    } else {
                        switch (item) {
                            case 1:
                                this._result.setDG(1, toByteArray(cfInputStream));
                                break;
                            case 2:
                                this._result.setDG(2, toByteArray(cfInputStream));
                            case 3:
                            default:
                                break;
                            case 4:
                                this._result.setDG(4, toByteArray(cfInputStream));
                                break;
                            case 5:
                                this._result.setDG(5, toByteArray(cfInputStream));
                                break;
                            case 6:
                                this._result.setDG(6, toByteArray(cfInputStream));
                                break;
                            case 7:
                                this._result.setDG(7, toByteArray(cfInputStream));
                                break;
                            case 8:
                                this._result.setDG(8, toByteArray(cfInputStream));
                                break;
                            case 9:
                                this._result.setDG(9, toByteArray(cfInputStream));
                                break;
                            case 10:
                                this._result.setDG(10, toByteArray(cfInputStream));
                                break;
                            case 11:
                                this._result.setDG(11, toByteArray(cfInputStream));
                                break;
                            case 12:
                                this._result.setDG(12, toByteArray(cfInputStream));
                                break;
                            case 13:
                                this._result.setDG(13, toByteArray(cfInputStream));
                                break;
                            case 14:
                                this._dgSecurityInfoFile = new DG14File(cfInputStream);
                                this._result.setDG(14, this._dgSecurityInfoFile.getEncoded());
                                break;
                            case 15:
                                this._dgPublicKeyFile = new DG15File(cfInputStream);
                                this._result.setDG(15, this._dgPublicKeyFile.getEncoded());
                                break;
                            case 16:
                                this._result.setDG(16, toByteArray(cfInputStream));
                        }
                    }
                }
            }
        } catch (Exception var10) {
            Log.e(TAG, "Error", var10);
            this._result.setCode(ResultCode.CARD_LOST_CONNECTION);
        }

        return this._result.getCode();
    }

    private ResultCode chipAuthentication() {
        try {
            Log.d(TAG, "Start Chip Authentication...");
            Collection<SecurityInfo> dg14FileSecurityInfos = this._dgSecurityInfoFile.getSecurityInfos();
            Iterator var2 = dg14FileSecurityInfos.iterator();

            while (var2.hasNext()) {
                SecurityInfo securityInfo = (SecurityInfo) var2.next();
                if (securityInfo instanceof ChipAuthenticationPublicKeyInfo) {
                    ChipAuthenticationPublicKeyInfo publicKeyInfo = (ChipAuthenticationPublicKeyInfo) securityInfo;
                    BigInteger keyId = publicKeyInfo.getKeyId();
                    PublicKey publicKey = publicKeyInfo.getSubjectPublicKey();
                    String oid = publicKeyInfo.getObjectIdentifier();
                    this._service.doEACCA(keyId, "0.4.0.127.0.7.2.2.3.2.4", oid, publicKey);
                    Log.d(TAG, "Chip Authentication: Success");
                    return ResultCode.SUCCESS;
                }
            }
        } catch (Exception var8) {
            Log.e(TAG, "Error", var8);
            this._result.setCode(ResultCode.CARD_LOST_CONNECTION);
            return ResultCode.CARD_LOST_CONNECTION;
        }

        System.out.println("Chip Authentication: Failed");
        this._result.setCode(ResultCode.SUCCESS_WITH_WARNING);
        return ResultCode.SUCCESS_WITH_WARNING;
    }

    private boolean verifyAA(PublicKey publicKey, String digestAlgorithm, String signatureAlgorithm, byte[] challenge, byte[] response) {
        try {
            String pubKeyAlgorithm = publicKey.getAlgorithm();
            if ("RSA".equals(pubKeyAlgorithm)) {
                Log.w(TAG, "Unexpected algorithms for RSA AA: digest algorithm = " + digestAlgorithm + ", signature algorithm = " + signatureAlgorithm);
                MessageDigest rsaAADigest = MessageDigest.getInstance(digestAlgorithm);
                Signature rsaAASignature = Signature.getInstance(signatureAlgorithm, JMRTDSecurityProvider.getSpongyCastleProvider());
                RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
                Cipher rsaAACipher = Cipher.getInstance("RSA/NONE/NoPadding");
                rsaAACipher.init(2, rsaPublicKey);
                rsaAASignature.initVerify(rsaPublicKey);
                int digestLength = rsaAADigest.getDigestLength();
                if (digestLength != 20) {
                    return false;
                } else {
                    byte[] plaintext = rsaAACipher.doFinal(response);
                    byte[] m1 = Util.recoverMessage(digestLength, plaintext);
                    rsaAASignature.update(m1);
                    rsaAASignature.update(challenge);
                    boolean bResult = rsaAASignature.verify(response);
                    Log.e(TAG, "Verify Active Auth: " + bResult);
                    return bResult;
                }
            } else if (!"EC".equals(pubKeyAlgorithm) && !"ECDSA".equals(pubKeyAlgorithm)) {
                Log.e(TAG, "Unsupported AA public key type " + publicKey.toString());
                return false;
            } else {
                Signature ecdsaAASignature = Signature.getInstance("SHA256withECDSA", JMRTDSecurityProvider.getSpongyCastleProvider());
                MessageDigest ecdsaAADigest = MessageDigest.getInstance("SHA-256");
                ECPublicKey ecdsaPublicKey = (ECPublicKey) publicKey;
                if (ecdsaAASignature == null || signatureAlgorithm != null && !signatureAlgorithm.equals(ecdsaAASignature.getAlgorithm())) {
                    Log.w(TAG, "Re-initializing ecdsaAASignature with signature algorithm " + signatureAlgorithm);
                    ecdsaAASignature = Signature.getInstance(signatureAlgorithm);
                }

                if (ecdsaAADigest == null || digestAlgorithm != null && !digestAlgorithm.equals(ecdsaAADigest.getAlgorithm())) {
                    Log.w(TAG, "Re-initializing ecdsaAADigest with digest algorithm " + digestAlgorithm);
                    ecdsaAADigest = MessageDigest.getInstance(digestAlgorithm);
                }

                ecdsaAASignature.initVerify(ecdsaPublicKey);
                if (response.length % 2 != 0) {
                    Log.w(TAG, "Active Authentication response is not of even length");
                }

                int l = response.length / 2;
                BigInteger r = Util.os2i(response, 0, l);
                BigInteger s = Util.os2i(response, l, l);
                ecdsaAASignature.update(challenge);

                try {
                    DERSequence asn1Sequence = new DERSequence(new ASN1Encodable[]{new ASN1Integer(r), new ASN1Integer(s)});
                    return ecdsaAASignature.verify(asn1Sequence.getEncoded());
                } catch (IOException var15) {
                    Log.e(TAG, "Unexpected exception during AA signature verification with ECDSA");
                    return false;
                }
            }
        } catch (Exception var16) {
            Log.e(TAG, "Error", var16);
            return false;
        }
    }

    private ResultCode activeAuthentication() {
        try {
            Log.d(TAG, "Start Active Authentication...");
            PublicKey pubKey = this._dgPublicKeyFile.getPublicKey();
            String pubKeyAlgorithm = pubKey.getAlgorithm();
            String digestAlgorithm = "SHA1";
            String signatureAlgorithm = "SHA1WithRSA/ISO9796-2";
            if ("EC".equals(pubKeyAlgorithm) || "ECDSA".equals(pubKeyAlgorithm)) {
                ArrayList<ActiveAuthenticationInfo> activeAuthenticationInfoList = new ArrayList();
                Collection<SecurityInfo> dg14FileSecurityInfos = this._dgSecurityInfoFile.getSecurityInfos();
                Iterator var7 = dg14FileSecurityInfos.iterator();

                while (true) {
                    if (!var7.hasNext()) {
                        int activeAuthenticationInfoCount = activeAuthenticationInfoList.size();
                        if (activeAuthenticationInfoCount < 1) {
                            Log.e(TAG, "Not found active authentication info in EF.DG14");
                            this._result.setCode(ResultCode.SUCCESS_WITH_WARNING);
                            return ResultCode.SUCCESS_WITH_WARNING;
                        }

                        if (activeAuthenticationInfoCount > 1) {
                            Log.d(TAG, "Found activeAuthenticationInfoCount in EF.DG14, expected 1.");
                        }

                        ActiveAuthenticationInfo activeAuthenticationInfo = (ActiveAuthenticationInfo) activeAuthenticationInfoList.get(0);
                        String signatureAlgorithmOID = activeAuthenticationInfo.getSignatureAlgorithmOID();
                        signatureAlgorithm = ActiveAuthenticationInfo.lookupMnemonicByOID(signatureAlgorithmOID);
                        digestAlgorithm = Util.inferDigestAlgorithmFromSignatureAlgorithm(signatureAlgorithm);
                        break;
                    }

                    SecurityInfo securityInfo = (SecurityInfo) var7.next();
                    if (securityInfo instanceof ActiveAuthenticationInfo) {
                        activeAuthenticationInfoList.add((ActiveAuthenticationInfo) securityInfo);
                    }
                }
            }

            int challengeLength = 8;
            byte[] challenge = new byte[challengeLength];
            SecureRandom.getInstanceStrong().nextBytes(challenge);
            AAResult aaResult = this._service.doAA(pubKey, this._sodFile.getDigestAlgorithm(), this._sodFile.getSignerInfoDigestAlgorithm(), challenge);
            if (this.verifyAA(pubKey, digestAlgorithm, signatureAlgorithm, challenge, aaResult.getResponse())) {
                Log.d(TAG, "Active Authentication: succeeded");
                return ResultCode.SUCCESS;
            }

            Log.d(TAG, "Active Authentication: Failed");
        } catch (Exception var11) {
            Log.e(TAG, "Error", var11);
            this._result.setCode(ResultCode.CARD_LOST_CONNECTION);
            return ResultCode.CARD_LOST_CONNECTION;
        }

        this._result.setCode(ResultCode.SUCCESS_WITH_WARNING);
        return this._result.getCode();
    }

    private void closeALl() {
        try {
            if (this._service != null) {
                this._service.close();
            }
        } catch (Exception var3) {
            Log.d(TAG, "Exception: " + var3.getMessage());
        }

        try {
            if (this._card != null) {
                this._card.close();
            }
        } catch (Exception var2) {
            Log.d(TAG, "Exception: " + var2.getMessage());
        }

    }

    private byte[] toByteArray(InputStream inputStream) throws IOException {
        final int bufLen = 4 * 0x400; // 4KB
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;

        try {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                    outputStream.write(buf, 0, readLen);

                return outputStream.toByteArray();
            }
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            if (exception == null) inputStream.close();
            else try {
                inputStream.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
        }
    }
}
