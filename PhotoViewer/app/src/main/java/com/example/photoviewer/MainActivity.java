package com.example.photoviewer; // ★ 본인 프로젝트 패키지명 확인 ★

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView textView;

    // 에뮬레이터에서 로컬 컴퓨터(장고 서버)로 접속하기 위한 주소
    String site_url = "http://172.21.19.38:8000";
    JSONObject post_json;
    String imageUrl = null;

    CloadImage taskDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
    }

    // 동기화 버튼 클릭 이벤트
    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download 시작", Toast.LENGTH_SHORT).show();
    }

    // 새로운 이미지 게시 버튼 클릭 이벤트 (다음 단계에서 구현 예정)
    public void onClickUpload(View v) {
        PutPost taskUpload = new PutPost();
        taskUpload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Upload 시도 중...", Toast.LENGTH_SHORT).show();
    }

    // 서버 통신 및 이미지 다운로드 비동기 클래스
    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {
        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            List<Bitmap> bitmapList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();

                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(1000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);

                    // JSON 배열을 순회하며 이미지 URL 파싱 및 비트맵 변환
                    for (int i = 0; i < aryJson.length(); i++) {
                        post_json = (JSONObject) aryJson.get(i);

                        // "image" 항목 값 가져오기 (null 처리 강화)
                        imageUrl = post_json.optString("image", "null");

                        if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {

                            // [핵심 수정 부분]
                            // 장고 서버가 보내준 이미지 주소의 IP(127.0.0.1)를
                            // 안드로이드 에뮬레이터 접근용 IP(172.21.19.38)로 강제 치환
                            imageUrl = imageUrl.replace("127.0.0.1", "172.21.19.38");
                            imageUrl = imageUrl.replace("localhost", "172.21.19.38");

                            URL myImageUrl = new URL(imageUrl);
                            HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();

                            // 이미지 스트림 읽기
                            InputStream imgStream = imgConn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);

                            if (imageBitmap != null) {
                                bitmapList.add(imageBitmap);
                            }
                            imgStream.close();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return bitmapList;
        }

        @Override
        protected void onPostExecute(List<Bitmap> images) {
            if (images.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText("이미지 로드 성공! (총 " + images.size() + "장)");

                // 리사이클러뷰에 이미지 목록 어댑터 연결
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                ImageAdapter adapter = new ImageAdapter(images);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private class PutPost extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                // 하드코딩된 JSON 데이터 전송 (작성자 번호 1, 제목, 내용)
                String jsonInputString = "{\"author\": 1, \"title\": \"안드로이드 업로드\", \"text\": \"앱에서 작성된 테스트 글입니다.\"}";

                java.io.OutputStream os = conn.getOutputStream();
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    return "업로드 성공!";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "업로드 실패";
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            // 업로드가 완료되면 목록을 다시 불러와 화면을 갱신합니다.
            onClickDownload(null);
        }
    }
}