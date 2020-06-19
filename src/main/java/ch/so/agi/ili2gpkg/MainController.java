package ch.so.agi.ili2gpkg;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.gui.Config;
import ch.ehi.ili2gpkg.GpkgMain;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.views.View;

@Controller("/")
public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Get(uri="/ping", produces="text/plain")
    public String index() {
        return "ili2gpkg-web-service";
    }
        
    @Get("/")
    @View("upload")
    public HttpStatus upload() {
        return HttpStatus.OK;
    }

    @Post(value = "/", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_OCTET_STREAM) 
    @View("upload")
    public HttpResponse<?> validate(CompletedFileUpload file, Optional<String> doStrokeArcs, Optional<String> doNameByTopic, 
            Optional<String> doDisableValidation) {
        try {           
            if (file.getSize() == 0 || file.getFilename().trim().equalsIgnoreCase("") || file.getName() == null) {
                log.warn("No file was uploaded. Redirecting to starting page.");
                return HttpResponse.seeOther(URI.create("/"));
            }
            
            String strokeArcs = doStrokeArcs.orElse(null);
            String nameByTopic = doNameByTopic.orElse(null);
            String disableValidation = doDisableValidation.orElse(null);
            log.info(disableValidation);
            
            File tmpFolder = Files.createTempDirectory("ili2gpkgws-").toFile();
            if (!tmpFolder.exists()) {
                tmpFolder.mkdirs();
            }
            log.info("tmpFolder {}", tmpFolder.getAbsolutePath());

            Path uploadFilePath = Paths.get(tmpFolder.toString(), file.getFilename());
            byte[] bytes = file.getBytes();
            Files.write(uploadFilePath, bytes);
            String uploadFileName = uploadFilePath.toFile().getAbsolutePath();
            log.info("uploadFileName {}", uploadFileName);

            
            Config settings = createConfig();
            settings.setFunction(Config.FC_IMPORT);
            settings.setDoImplicitSchemaImport(true);
            settings.setDefaultSrsCode("2056"); // TODO Hardcodieren für Naturgefahren.

            if (strokeArcs != null) {
                settings.setStrokeArcs(settings, settings.STROKE_ARCS_ENABLE);
            }
            
            if (nameByTopic != null) {
                settings.setNameOptimization(settings.NAME_OPTIMIZATION_TOPIC);
            }
            
            if (disableValidation != null) {
                settings.setValidation(false);
            }

            if (Ili2db.isItfFilename(uploadFileName)) {
                settings.setItfTransferfile(true);
            }

            String gpkgFileName = uploadFileName.substring(0, uploadFileName.length()-4) + ".gpkg";
            settings.setDbfile(gpkgFileName);

            settings.setDburl("jdbc:sqlite:" + settings.getDbfile());
            settings.setXtffile(uploadFileName);

            Ili2db.run(settings, null);

            
            
            
            return HttpResponse.ok().body("fubar");
            
//            Path tmpDirectory = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), FOLDER_PREFIX);
//            Path uploadFilePath = Paths.get(tmpDirectory.toString(), file.getFilename());
//
//            byte[] bytes = file.getBytes();
//            Files.write(uploadFilePath, bytes);
//            String uploadFileName = uploadFilePath.toFile().getAbsolutePath();
//            log.info(uploadFileName);
//            
//            String gpkgFileName = ili2gpkgService.convert(uploadFileName, strokeArcs);
//            
//            return HttpResponse.ok().header("content-disposition", "attachment; filename=" + new File(gpkgFileName).getName())
//                    .contentLength(new File(gpkgFileName).length())
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                    .body(new File(gpkgFileName));      
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return HttpResponse.badRequest("Something went wrong:\n\n" + e.getMessage());
        }
    }
    
    private Config createConfig() {
        Config settings = new Config();
        new GpkgMain().initConfig(settings);
        return settings;
    }
}