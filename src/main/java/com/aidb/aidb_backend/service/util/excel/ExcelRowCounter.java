package com.aidb.aidb_backend.service.util.excel;

import lombok.Getter;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;

public class ExcelRowCounter {

    public static int countRows(MultipartFile file) throws Exception {
        InputStream excelStream = file.getInputStream();
        OPCPackage pkg = OPCPackage.open(excelStream);
        XSSFReader reader = new XSSFReader(pkg);
        int totalRows = 0;

        XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();

        while (sheets.hasNext()) {
            InputStream sheetStream = sheets.next();
            RowCountingHandler handler = new RowCountingHandler();
            saxParser.parse(sheetStream, handler);
            totalRows += handler.getRowCount();
            sheetStream.close();
        }

        return totalRows;
    }

    @Getter
    private static class RowCountingHandler extends DefaultHandler {
        private int rowCount = 0;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("row".equals(qName)) {
                rowCount++;
            }
        }

    }
}
