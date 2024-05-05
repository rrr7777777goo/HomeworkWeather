package zerobase.weather.service;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.WeatherApplication;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.error.InvalidDate;
import zerobase.weather.repository.DateWeatherRepository;
import zerobase.weather.repository.DiaryRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DiaryService {

    @Value("${openweathermap.key}")
    private String apiKey_WeatherUrl;

    private String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apiKey_WeatherUrl;

    private String currentDateDataUrl = "http://worldtimeapi.org/api/timezone/Asia/Seoul";

    private LocalDate minimumDate = LocalDate.parse("1900-01-01");

    private final DiaryRepository diaryRepository;

    private final DateWeatherRepository dateWeatherRepository;

    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);


    public DiaryService(DiaryRepository diaryRepository, DateWeatherRepository dateWeatherRepository) {
        this.diaryRepository = diaryRepository;
        this.dateWeatherRepository = dateWeatherRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void saveWeatherData() {
        dateWeatherRepository.save(getWeatherFromApi());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createDiary(LocalDate date, String text) {
        logger.info("선택한 날짜 '" + date.toString() + "'의 일기 작성을 시작합니다.");
        isBeforeMinimumDate(date);
        LocalDate currentDate = isAfterCurrentDate(date);

        Diary nowDiary = new Diary();

        // 날씨 데이터 가져오기(API에서 가져오기 or DB에서 가져오기)
        DateWeather dateWeather = getDateWeather(date, currentDate);

        nowDiary.setDateWeather(dateWeather);
        nowDiary.setText(text);
        diaryRepository.save(nowDiary);
        logger.info("선택한 날짜 '" + date.toString() + "'의 일기 작성이 완료 되었습니다.");
    }

    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date) {
        logger.info("선택한 날짜 '" + date.toString() + "'의 모든 일기들을 불러옵니다.");
        isBeforeMinimumDate(date);
        isAfterCurrentDate(date);
        // logger.debug("read diary");
        return diaryRepository.findAllByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {
        logger.info("선택한 날짜 범위 '" + startDate.toString() + "' - '" + endDate.toString() +"'의 모든 일기들을 불러옵니다.");
        isBeforeMinimumDate(startDate);
        isAfterCurrentDate(startDate);
        isBeforeMinimumDate(endDate);
        isAfterCurrentDate(endDate);
        return diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    @Transactional
    public void updateDiary(LocalDate date, String text) {
        logger.info("일기 내용 수정을 시작합니다. (선택한 날짜 '" + date.toString() + "'의 첫번째 일기가 수정됩니다.)");
        isBeforeMinimumDate(date);
        isAfterCurrentDate(date);
        Diary nowDiary = diaryRepository.getFirstByDate(date);
        if(nowDiary == null) {
            logger.info("현재 수정하려는 날짜 '" + date.toString() + "'에 작성된 일기가 없습니다.");
        } else {
            nowDiary.setText(text);
            diaryRepository.save(nowDiary);
            logger.info("일기 내용 수정이 완료되었습니다.");
        }
    }

    @Transactional
    public void deleteDiary(LocalDate date) {
        logger.info("선택한 날짜 '" + date.toString() + "'의 모든 일기 제거를 시작합니다.");
        isBeforeMinimumDate(date);
        isAfterCurrentDate(date);
        diaryRepository.deleteAllByDate(date);
        logger.info("선택한 날짜 '" + date.toString() + "'의 모든 일기 제거가 완료되었습니다.");
    }

    private void isBeforeMinimumDate(LocalDate date) {
        if(date.compareTo(minimumDate) < 0) {
            logger.error(minimumDate.toString() + "보다 전의 날짜에는 일기를 작성하거나 조회할 수 없습니다.");
            throw new InvalidDate();
        }
    }
    private LocalDate isAfterCurrentDate(LocalDate date) {
        LocalDate currentDate = getCurrentDateDataFromApi();
        if(date.compareTo(currentDate) > 0) {
            logger.error("현재 날짜 " + currentDate.toString() + "보다 이후의 날짜(" + date.toString() + ")에는 일기를 작성하거나 조회할 수 없습니다.");
            throw new InvalidDate();
        }
        return currentDate;
    }

    private DateWeather getDateWeather(LocalDate date, LocalDate currentDate) {
        List<DateWeather> dateWeatherListFromDB = dateWeatherRepository.findAllByDate(date);
        if(dateWeatherListFromDB.size() == 0) {
            if (date.compareTo(currentDate) == 0) {
                // 현재 날짜에 대한 날씨 정보가 없는 경우 새로 api에서 정보를 가져와야 한다.
                return getWeatherFromApi();
            }
            else {
                // 과거 날짜에 대한 날씨 정보가 없는 경우 아래와 같이 값을 입력한다.
                logger.info("해당 날짜 '" + date.toString() + "'의 날씨 정보를 가져올 수 없습니다.");
                DateWeather dateWeatherNODATA = new DateWeather(date, "NO DATA", "NO DATA", 0.0);
                return dateWeatherNODATA;
            }
        } else {
            return dateWeatherListFromDB.get(0);
        }
    }

    private DateWeather getWeatherFromApi() {
        // open weather map에서 날씨 데이터 가져오기
        String weatherData = getStringFromApiUrl(weatherUrl);

        DateWeather dateWeather = new DateWeather();
        // 날씨 json 파싱하기
        Map<String, Object> parsedWeather = parseWeather(weatherData);
        dateWeather.setDate(LocalDate.now());
        dateWeather.setWeather(parsedWeather.get("main").toString());
        dateWeather.setIcon(parsedWeather.get("icon").toString());
        dateWeather.setTemperature((Double) parsedWeather.get("temp"));

        return dateWeather;
    }

    private LocalDate getCurrentDateDataFromApi() {
        // worldtimeapi에서 현재 날짜 정보 가져오기
        String currentDateData = getStringFromApiUrl(currentDateDataUrl);

        DateWeather dateWeather = new DateWeather();

        // 날씨 json 파싱하기
        Map<String, Object> parsedCurrentDateData = parseCurrentDateData(currentDateData);
        String currrentDate = parsedCurrentDateData.get("datetime").toString().substring(0, 10);

        return LocalDate.parse(currrentDate);
    }

    // 외부 api를 통해서 정보를 가져오는 메서드
    private String getStringFromApiUrl (String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            BufferedReader br;
            if(responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            String inputLine;
            StringBuilder response = new StringBuilder();

            while((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();

            return response.toString();
        } catch (Exception e) {
            logger.error(apiUrl + "api 호출 도중 에러가 발생하였습니다.");
            throw new RuntimeException(e);
        }
    }

    // 날씨 정보를 파싱하는 메서드
    private Map<String, Object> parseWeather(String jsonString) {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) jsonParser.parse(jsonString);
        } catch (ParseException e) {
            throwRunTimeException("parseWeather 파싱 도중 문제가 발생하였습니다.", e);
        }
        Map<String, Object> resultMap = new HashMap<>();

        JSONObject mainData = (JSONObject) jsonObject.get("main");
        resultMap.put("temp", mainData.get("temp"));
        JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
        JSONObject weatherData = (JSONObject) weatherArray.get(0);
        resultMap.put("main", weatherData.get("main"));
        resultMap.put("icon", weatherData.get("icon"));
        return resultMap;
    }

    // 현재 날짜 정보를 파싱하는 메서드
    private Map<String, Object> parseCurrentDateData(String jsonString) {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) jsonParser.parse(jsonString);
        } catch (ParseException e) {
            throwRunTimeException("CurrentDateData 파싱 도중 문제가 발생하였습니다.", e);
        }
        Map<String, Object> resultMap = new HashMap<>();

        resultMap.put("datetime", jsonObject.get("datetime"));
        return resultMap;
    }

    // 런타임 에러 생성 메서드
    public void throwRunTimeException(String errorMessage, Exception e) {
        logger.error(errorMessage);
        throw new RuntimeException(e);
    }
}
