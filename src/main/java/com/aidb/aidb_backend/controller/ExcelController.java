package com.aidb.aidb_backend.controller;

import com.aidb.aidb_backend.exception.ExcelValidationException;
import com.aidb.aidb_backend.exception.http.HttpException;
import com.aidb.aidb_backend.model.dto.ExcelDataDto;
import com.aidb.aidb_backend.security.authorization.FirebaseAuthService;
import com.aidb.aidb_backend.service.database.postgres.UserFileDataService;
import com.aidb.aidb_backend.service.util.excel.ExcelDataValidatorService;
import com.aidb.aidb_backend.service.util.excel.ExcelParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user-file-data")
public class ExcelController {

    @Autowired
    ExcelDataValidatorService dataValidatorService;

    @Autowired
    ExcelParserService parserService;

    @Autowired
    UserFileDataService userFileDataService;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    private static final Logger logger = LoggerFactory.getLogger(ExcelController.class);

    @PostMapping("/upload")
    public ResponseEntity<Object> uploadExcel(@RequestHeader("Authorization") String authToken, @RequestParam("file")MultipartFile file){
        try {
            String userId = firebaseAuthService.authorizeUser(authToken);
            ExcelDataDto excelData = parserService.parseExcelFile(file.getInputStream());
            dataValidatorService.validateData(excelData);
            userFileDataService.createTablesAndInsertData(excelData);

            return ResponseEntity.ok(excelData);
        } catch (HttpException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (ExcelValidationException e) {
            logger.error(e.getMessage(), e.getHttpStatus());
            return new ResponseEntity<>(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>("Unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
