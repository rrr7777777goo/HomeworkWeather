package zerobase.weather.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.tomcat.jni.Local;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import zerobase.weather.domain.Diary;
import zerobase.weather.service.DiaryService;

import java.time.LocalDate;
import java.util.List;


@RestController
public class DiaryController {
    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @ApiOperation(value = "일기 텍스트와 날씨를 이용해서 DB에 일기를 저장합니다.", notes= "날짜는 1900-01-01부터 현재 날짜까지 사용이 가능하며, 과거의 날씨정보가 존재하지 않을 경우에는 일기에 날씨정보가 저장되지 않습니다.")
    @PostMapping("/create/diary")
    void createDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @ApiParam(value="작성 날짜", example="2024-05-05") LocalDate date,
                     @RequestBody @ApiParam(value="일기 내용", example="일기 내용입니다.") String text) {
        diaryService.createDiary(date, text);
    }

    @ApiOperation(value = "선택한 날짜의 모든 일기 데이터들을 가져옵니다.", notes= "날짜는 1900-01-01부터 현재 날짜까지 사용이 가능합니다.")
    @GetMapping("/read/diary")
    List<Diary> readDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @ApiParam(value="조회할 날짜", example="2024-05-05") LocalDate date) {
        return diaryService.readDiary(date);
    }

    @ApiOperation(value = "선택한 기간중의 모든 일기 데이터를 가져옵니다.", notes= "날짜는 1900-01-01부터 현재 날짜까지 사용이 가능합니다.")
    @GetMapping("/read/diaries")
    List<Diary> readDiaries(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @ApiParam(value="조회할 기간의 첫번째날", example="2024-05-05") LocalDate startDate,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @ApiParam(value="조회할 기간의 마지막날", example="2024-05-05") LocalDate endDate) {
        return diaryService.readDiaries(startDate, endDate);
    }

    @ApiOperation(value = "선택한 날짜의 첫번째로 작성된 일기 데이터 내용을 수정합니다.", notes= "날짜는 1900-01-01부터 현재 날짜까지 사용이 가능합니다.")
    @PutMapping("/update/diary")
    void updateDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  @ApiParam(value="수정 날짜", example="2024-05-05") LocalDate date,
                     @RequestBody @ApiParam(value="일기 내용", example="일기 내용입니다.") String text) {
        diaryService.updateDiary(date, text);
    }

    @ApiOperation(value = "선택한 날짜의 모든 일기 데이터를 삭제합니다." , notes= "날짜는 1900-01-01부터 현재 날짜까지 사용이 가능합니다.")
    @DeleteMapping("/delete/diary")
    void deleteDiary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @ApiParam(value="삭제 날짜", example="2024-05-05") LocalDate date) {
        diaryService.deleteDiary(date);
    }
}
