package com.hackathon.project.domain.Roadmap;

import com.hackathon.project.domain.GeminiService;
import com.hackathon.project.domain.Roadmap.dto.ExcelParseDTO;
import com.hackathon.project.domain.Roadmap.dto.RoadmapAiResponseDTO;
import com.hackathon.project.domain.Roadmap.dto.RoadmapCreateRequestDTO;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final GeminiService geminiService;

    public List<ExcelParseDTO> parse(MultipartFile file) {
        List<ExcelParseDTO> results = new ArrayList<>();

        try (InputStream is = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0); // 첫 시트

            // 0번째 row는 헤더라고 가정하고 1번째부터 시작
            for (int i = 4; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                ExcelParseDTO dto = ExcelParseDTO.builder()
                    .courseCode(getString(row.getCell(3)))        // 학수번호
                    .courseName(getString(row.getCell(4)))        // 교과목명
                    .courseType(getString(row.getCell(5)))        // 이수구분
                    .teachingArea(getString(row.getCell(6)))      // 교직영역
                    .selectedArea(getString(row.getCell(7)))      // 선택영역
                    .credits(getInt(row.getCell(8)))               // 학점
                    .evaluationType(getString(row.getCell(9)))    // 평가방식
                    .grade(getString(row.getCell(10)))             // 등급
                    .gradePoint(getDouble(row.getCell(11)))        // 평점
                    .departmentCode(getString(row.getCell(12)))    // 개설학과코드
                    .build();

                results.add(dto);
            }

        } catch (IOException e) {
            throw new RuntimeException("엑셀 파싱 실패", e);
        }

        return results;
    }

    private String getString(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private int getInt(Cell cell) {
        if (cell == null) {
            return 0;
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                String value = cell.getStringCellValue().trim();
                yield value.isEmpty() ? 0 : Integer.parseInt(value);
            }
            default -> 0;
        };
    }

    private double getDouble(Cell cell) {
        if (cell == null) {
            return 0.0;
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                String value = cell.getStringCellValue().trim();
                yield value.isEmpty() ? 0.0 : Double.parseDouble(value);
            }
            default -> 0.0;
        };
    }

    public RoadmapAiResponseDTO generateRoadmap(RoadmapCreateRequestDTO requestDTO) {
        return geminiService.askRoadMap(requestDTO);
    }
}
