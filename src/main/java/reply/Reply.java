package reply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import constants.Constants;

public class Reply {

	public void reply(String body) throws IOException {
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
		httpPost.setHeader("Authorization", "Bearer " + Constants.CHANNEL_ACCESS_TOKEN);
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
