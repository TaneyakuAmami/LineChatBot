package function;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.codec.binary.Base64;

import constants.Constants;

public class Function {

	public void authorization(HttpServletRequest request, HttpServletResponse response, String body) throws IOException {
		try (Stream<String> stream = request.getReader().lines()) {
			String signature = request.getHeader("X-Line-Signature");
			body = stream.reduce((s1, s2) -> s1 + "¥n" + s2).orElse("");
			SecretKeySpec key = new SecretKeySpec(Constants.CHANNEL_SECRET.getBytes(), Constants.HMAC_SHA256);
			Mac mac = Mac.getInstance(Constants.HMAC_SHA256);
			mac.init(key);
			byte[] source = body.getBytes(StandardCharsets.UTF_8);
			String createdSignature = Base64.encodeBase64String(mac.doFinal(source));

			if (!signature.equals(createdSignature)) {
				//LINEからのリクエストじゃない場合の処理
				//常に200を返す
				response.setStatus(200);
				return;
			}
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
		}
		System.out.println("認証OK");
	}

}
