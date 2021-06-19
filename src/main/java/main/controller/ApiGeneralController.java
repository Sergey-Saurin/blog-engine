package main.controller;

import main.api.request.CommentRequest;
import main.api.request.GlobalVariablesRequest;
import main.api.request.ModerationStatusInstallRequest;
import main.api.request.ProfileRequest;
import main.dto.*;
import main.service.GeneralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api")
public class ApiGeneralController {
    /////////////////////////////////////////////////////////////
    /**
     * GET
     */

    @Autowired
    private GeneralService generalService;

    @GetMapping("/init")
    public ResponseEntity<HeaderSite> apiInit() {
        HeaderSite headerSite = generalService.getDataForHeaderSite();
        if (headerSite == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(headerSite, HttpStatus.OK);
    }

    @GetMapping("/tag")
    public ResponseEntity<Tags> getTag(@RequestParam(defaultValue = "") String query) {
        Tags tags = generalService.getTags(query);
        return new ResponseEntity<>(tags, HttpStatus.OK);
    }

    @GetMapping("/calendar")
    public ResponseEntity<PostsByDate> getCalendar(@RequestParam(value = "year") Integer year) {
        PostsByDate postsByDate = generalService.getPostsByDateForCalendar(year);
        return new ResponseEntity<>(postsByDate, HttpStatus.OK);
    }

    @GetMapping("/statistics/my")
    public ResponseEntity<Statistics> getStatisticsMy(Principal principal) {

        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Statistics statistics = generalService.getStatisticsMy(principal);
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }

    @GetMapping("/statistics/all")
    public ResponseEntity<Statistics> getStatisticsAll(Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return generalService.getStatisticsAll(principal);
    }

    @GetMapping("/settings")
    public ResponseEntity<GlobalVariables> getSettings() {
        GlobalVariables globalVariables = generalService.getGlobalSettings();
        if (globalVariables == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(globalVariables, HttpStatus.OK);
    }

    /////////////////////////////////////////////////////////////

    /**
     * POST
     */
    @PostMapping("/comment")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<?> postComment(@RequestBody CommentRequest commentRequest,
                                         Principal principal) {
        return generalService.saveNewComment(commentRequest, principal);
    }

    @PostMapping("/image")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<?> postImage(@RequestPart MultipartFile image, Principal principal) {

        String pathImage = generalService.uploadImage(image);
        if (pathImage == null) {
            UploadImageError error = new UploadImageError();
            error.setErrors(error.new Error());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(pathImage, HttpStatus.OK);
    }

    @PostMapping("/moderation")
    @PreAuthorize("hasAuthority('user:moderate')")
    public ResponseEntity<PostSave> postModeration(@RequestBody ModerationStatusInstallRequest modStatusInstallRequest,
                                                   Principal principal) {
        PostSave postSave = generalService.installModerationStatusForPost(modStatusInstallRequest, principal);
        return new ResponseEntity<>(postSave, HttpStatus.OK);
    }

    /**
     * Не нашел способа как из multipart/form-data сразу в Класс ProfileRequest поместить данные
     * На форумах нашел тольео такой вариант как каждую переменную получать по отдельности
     */
    @PostMapping(value = "/profile/my", consumes = {"multipart/form-data"})
    public ResponseEntity<Profile> postProfileMyWithPhoto(@RequestPart("photo") MultipartFile photo,
                                                          @RequestPart("name") String name,
                                                          @RequestPart("email") String email,
                                                          @RequestPart(value = "password", required = false) String password,
                                                          @RequestPart(value = "removePhoto", required = false) String removePhoto,
                                                          Principal principal,
                                                          HttpServletRequest request) throws IOException {
        /**Создать объект ProfileRequest через конструктор, так как получает все переменные по частям   */
        ProfileRequest profileRequest = new ProfileRequest(name, email, password, Integer.parseInt(removePhoto));
        Profile profile = generalService.changeProfile(profileRequest, principal, photo, request);
        return new ResponseEntity<>(profile, HttpStatus.OK);
    }

    @PostMapping(value = "/profile/my", consumes = {"application/json"})
    public ResponseEntity<Profile> postProfileMyWithoutPhoto(@RequestBody ProfileRequest profileRequest,
                                                             Principal principal,
                                                             HttpServletRequest request) throws IOException {

        Profile profile = generalService.changeProfile(profileRequest, principal, null, request);
        return new ResponseEntity<>(profile, HttpStatus.OK);
    }
    /////////////////////////////////////////////////////////////

    /*** PUT*/
    @PutMapping("/settings")
    @PreAuthorize("hasAuthority('user:moderate')")
    public void putSettings(@RequestBody GlobalVariablesRequest globalVariablesRequest) {
        generalService.saveGlobalVariables(globalVariablesRequest);
    }

}












