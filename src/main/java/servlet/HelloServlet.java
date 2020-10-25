package servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tomcat.util.codec.binary.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HelloServlet extends HttpServlet{
	public static final String CHANNEL_SECRET = "15855ce25980cfecfe68b08beb3d043e";
	public static final String CHANNEL_ACCESS_TOKEN = "7oanhLcZMzS1vtAUIPUMXXqesPTgpMOF00RD5qOk26o7blTx/"
			+ "axpeXNr9WKfaSVmC4ov8U8Tgo5WflI0nJH19oCP"
			+ "pts2V78XvV/QZ5VdbpGKU3Ij9DVAC3RZLGeYqPq"
			+ "WgFjsh/aVymKxaAFoPvzlsgdB04t89/1O/w1cDnyilFU=";
	public static final String HMAC_SHA256 = "HmacSHA256";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException{

		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();

		out.println("<h1>Hello World</h1>");
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		//署名認証
		String body = null;
		try (Stream<String> stream = request.getReader().lines()) {
			String signature = request.getHeader("X-Line-Signature");
			body = stream.reduce((s1, s2) -> s1 + "¥n" + s2).orElse("");
			SecretKeySpec key = new SecretKeySpec(CHANNEL_SECRET.getBytes(), HMAC_SHA256);
			Mac mac = Mac.getInstance(HMAC_SHA256);
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

		//応答メッセージ作成
		ObjectMapper mapper = new ObjectMapper();
		JsonNode events = mapper.readTree(body).path("events");
		//リプライの種類
		String type = events.path(0).path("message").path("type").asText(null);
		//テキスト
		String query = events.path(0).path("message").path("text").asText(null);
		//返信用Token
		String replyToken = events.path(0).path("replyToken").asText(null);

		//リプライを送る
		HttpPost httpPost = new HttpPost("https://api.line.me/v2/bot/message/reply");
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setHeader("Authorization", "Bearer " + CHANNEL_ACCESS_TOKEN);
		//返信用のJSON
		String replybody = String.format("{\"replyToken\":\"%s\", \"messages\":[{\"type\":\"text\", \"text\":\"リプライありがとう！\"}]}", replyToken);
		StringEntity params = new StringEntity(replybody, StandardCharsets.UTF_8);
		httpPost.setEntity(params);
		System.out.println("リプライ送信");
		try (CloseableHttpClient client = HttpClients.createDefault();
				CloseableHttpResponse resp = client.execute(httpPost);
				BufferedReader br = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(),
						StandardCharsets.UTF_8))) {
			int statusCode = resp.getStatusLine().getStatusCode();
			System.out.println(statusCode);
			switch (statusCode) {
			case 200:
				br.readLine();
				break;
			default:
			}
		} catch (final ClientProtocolException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		System.out.println("処理終了");
	}
}
