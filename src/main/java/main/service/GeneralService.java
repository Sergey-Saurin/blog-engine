package main.service;

import main.GeneralMethods;
import main.api.request.CommentRequest;
import main.api.request.GlobalVariablesRequest;
import main.api.request.ModerationStatusInstallRequest;
import main.api.request.ProfileRequest;
import main.constructorforquery.DateWithQuantityPosts;
import main.constructorforquery.TagsAndWeight;
import main.dto.*;
import main.enums.ModerationStatus;
import main.model.GlobalVariable;
import main.model.Post;
import main.model.PostComment;
import main.model.User;
import main.repository.*;
import org.imgscalr.Scalr;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static main.GlobalVariables.*;

@Service
public class GeneralService {
    private final HeaderSite headerSite;
    private final GlobalVariablesRepository globalVariablesRepository;
    private final TagRepository tagRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public GeneralService(HeaderSite headerSite,
                          GlobalVariablesRepository globalVariablesRepository,
                          TagRepository tagRepository,
                          PostCommentRepository postCommentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.headerSite = headerSite;
        this.globalVariablesRepository = globalVariablesRepository;
        this.tagRepository = tagRepository;
        this.postCommentRepository = postCommentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public HeaderSite getDataForHeaderSite() {
        return headerSite;
    }

    public GlobalVariables getGlobalSettings() {
        List<GlobalVariable> gList = globalVariablesRepository.findAll();
        //global ?????????????????? ???????????????????? ???????? ?????????? ???????????????????????? ?????????????????? ???? ???????? g.getCode() ????????????????
        Optional<GlobalVariable> global;
        //???????? ???? ???????? code ?? ???????????????? ???????????? ??????????????????, ???????????? ???????????? ???? ???????????? ????????, ???????? ???????? ?????????? ???????????? ?????????? ???????????? ?????????????????? ????????
        global = gList.stream().filter(g -> g.getCode().equals("MULTIUSER_MODE")).findFirst();
        //?????????????????? ?????????????? ???? ???????????????? ???? ???????? ?? ?????????????????????? ???????????????? MULTIUSER_MODE, ???????? ???? ?????????????? ?????????? false
        global.ifPresent(g -> MULTIUSER_MODE = g.getValue().equals("YES"));

        //???????????????????? POST_PREMODERATION ?? STATISTICS_IS_PUBLIC ???????????????????? ???????????????? ????????

        global = gList.stream().filter(g -> g.getCode().equals("POST_PREMODERATION")).findFirst();
        global.ifPresent(g -> POST_PREMODERATION = g.getValue().equals("YES"));

        global = gList.stream().filter(g -> g.getCode().equals("STATISTICS_IS_PUBLIC")).findFirst();
        global.ifPresent(g -> STATISTICS_IS_PUBLIC = g.getValue().equals("YES"));

        GlobalVariables globalVariables = new GlobalVariables(MULTIUSER_MODE, POST_PREMODERATION, STATISTICS_IS_PUBLIC);
        return globalVariables;
    }

    public Tags getTags(String query) {
        List<TagsAndWeight> listTags;
        //?????????????? ???????????? ??????????, ???????????? ???? ?????? ????????????, ???????? ???? ?????????????????? ??????????????
        if (query.equals("")) {
            listTags = tagRepository.getAllTagsAndWeight(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        } else {
            listTags = tagRepository.getTagsAndWeightByQuery(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), query);
        }

        //???????? ???????????? ???????????? ?????????? ???????????? ????????????
        if (listTags.size() == 0) {
            return new Tags();
        }
        Tags tags = new Tags();
        //weightTagsList ???????????? ??????????, ?????????? ???????????????????? ???????????? ?????????????? ?????? ?? dto.tags
        ArrayList<Tags.weightTag> weightTagsList = new ArrayList<>();

        //?????????????? ???????????????????????? ????????, ???????????????? ?? ??????????????????????
        //?????????????????? ???? ???????????????? ???????????????????? ???????????? ?? ????????, ?????????????? ?????????? ???????????? ?????????????? ???????????? listTags
        //?????????? ???????????????????? ???????????? ?? ???????? ???? ?????????? ???????????????????? ????????????
        //maxWeight = quantityPostByTag / quantityAllPost
        //{
        double maxWeight = listTags.get(0).getQuantityPostByTag() / listTags.get(0).getQuantityAllPost();
        //}

        //?????????????? ?????? ?????????????? ??????????????
        for (TagsAndWeight obj : listTags) {
            //weightTag ???????????????? ???????????? dto.Tags
            Tags.weightTag weightTag = tags.new weightTag();
            //?????? ?????????????? ???????????????? ??????????????, ??.??. ?????????? ?????????? ???????????????? ??????????????, ?????? ???? ?????????? ???????????????????? ???????????? ???????? ?? ?????????? ?????????????????????? ????????????
            //?? ???? maxWeight, ???????????????????? ?????????????????????????? ??????, ?????? ?????????????? ???????????????? ?????????? ?????????? 1.0
            //?????? ?????? ?????????? quantityPostByTag / quantityAllPost = maxWeight
            //?????????????????? ???????????????? ?????????? ???????????? 1.0
            //maxWeight = quantityPostByTag / quantityAllPost
            //
            double weight = obj.getQuantityPostByTag() / obj.getQuantityAllPost() / maxWeight;
            weightTag.setName(obj.getName());
            //???????????????????? weight ?? ?????????????? 0.00 ?? ?????????? ?????????????? ?????????? ?????????????????? ???? ??????????
            weightTag.setWeight(String.format("%.2f", weight));

            weightTagsList.add(weightTag);
        }
        tags.setTags(weightTagsList);
        return tags;
    }

    public String uploadImage(MultipartFile image) {
        //allowedFormat ???????????????? ???????????????? ???? ???????????? ??????????
        boolean allowedFormat = checkFormatImage(Objects.requireNonNull(image.getContentType()));
        if (!allowedFormat) {
            return null;
        }
        //?????????????? ???????????? ????????????????
        InputStream inputStream = null;
        try {
            inputStream = image.getInputStream();
        } catch (IOException e) {
            return null;
        }
        //?????????????? ???????????????? ?????? ???????????????? ????????????????
        String pathFile = createPathImage(image.getOriginalFilename(), "upload");

        //???????????????? ???????? ????????????????
        try (FileOutputStream out = (new FileOutputStream(pathFile))) {
            byte[] bytes = inputStream.readAllBytes();
            out.write(bytes);
        } catch (IOException ioException) {
            return null;
        }
        return pathFile;
    }

    private boolean checkFormatImage(String contentType) {
        if (contentType.equals("image/jpg") ||
                contentType.equals("image/png"))
            return true;
        else
            return false;
    }

    private String createPathImage(String imageName, String nameRootFolder) {
        //?????????????? ?????? ???????????????? ?? ?????????????????? ???????? ?? ??????????
        StringBuilder pathImage = new StringBuilder();

        //?????????????? ???????????????? ?????????? ???????? ???? ??????
        Path path = Paths.get(pathImage.append(nameRootFolder).toString());
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(Path.of(pathImage.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //???????? ?????????????? ?????? ????????, ?????? ???????????????? ???????? ????????????????, ???????? ???????????????? ?? ?????????? ???????????? ?????? ????????,
        //?????????? ?????????????? ???? ??????????, ?????????? ???????????????????????? ?????????????????? ??????????
        for (int i = 0; i < 3; i++) {
            try {
                //?????????????? ?????? ???????????????? ???? ????????????????
                String nameDirectory = GeneralMethods.toGenerateRandomString(20);
                //???????????????? ???????????????? ?? ?????????? ?????????????? ?? ???????? ?? ???????????????? ?????? ????????????????
                Files.createDirectory(Path.of(pathImage.append("/").append(nameDirectory).toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        pathImage.append("/").append(imageName);

        return pathImage.toString();
    }

    public ResponseEntity<?> saveNewComment(CommentRequest commentRequest, Principal principal) {

        PostComment parentComment = postCommentRepository.findById(commentRequest.getParentId()).orElse(null);
        Post post = postRepository.findById(commentRequest.getPostId()).orElse(null);
        //?????????????????? ???????????????? ??????????????????????, ???????? ???? ?????????????????????????? ??????????????, ?????????????? ???????????? 400 ???????? ???? ???????????? parentComment ?????? post
        // ?????? JSON ???????? ???????????????? ??????????
        ResponseEntity<CommentError> checkComment = checkPropertyComment(commentRequest, post, parentComment);
        if (checkComment != null) {
            return checkComment;
        }

        //???????????????? ???????????????????????? ?????????????? ?????????????? ??????????????????????
        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user " + email + " not found"));

        PostComment postComment = new PostComment();
        postComment.setParentComment(parentComment);
        postComment.setPost(post);
        postComment.setText(commentRequest.getText());
        postComment.setTime(GeneralMethods.getCurrentTimeUTC());
        postComment.setUser(currentUser);

        postCommentRepository.save(postComment);
        CommentSuccess commentSuccess = new CommentSuccess(postComment.getId());

        return new ResponseEntity<>(commentSuccess, HttpStatus.OK);
    }

    private ResponseEntity<CommentError> checkPropertyComment(CommentRequest commentRequest, Post post, PostComment parentComment) {
        //?????????? ?????????????????????????? ??????????
        //???????? commentRequest.getParentId() == 0 ???????????? ?????????????????????? ?????????????? ?? ???????????? ??????????, ?????????? ?????????????????????? ?????????????? ?? ?????????????? ??????????????????????
        if (commentRequest.getParentId() != 0) {
            //???????? ?????????????????????? ???? ????????????, ???????????? BAD_REQUEST
            if (parentComment == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        //?????????? ??????????
        //???????? ???????? ???? ????????????, ???????????? BAD_REQUEST
        if (post == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //???????? ?????????????????????? ?????????????? ????????????????, ?????????????? ???????????? JSON
        if (commentRequest.getText().length() < 5) {
            CommentError commentError = new CommentError();
            commentError.setErrors(commentError.new Error());
            return new ResponseEntity<>(commentError, HttpStatus.OK);
        }
        //?????? ??????????????, ?????????? ?????????????????? ??????????????????????
        return null;
    }

    public PostSave installModerationStatusForPost(ModerationStatusInstallRequest modStatusInstallRequest, Principal principal) {
        //???????????????? ???????????????????????? ????????????????????, ???????????????? ???????????????????? ???? ?????????????????? PreAuthorize
        String email = principal.getName();
        User moderatorUser = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user " + email + " not found"));

        Post post = postRepository.findById(modStatusInstallRequest.getPostId()).orElse(null);
        //???????? ???????? ???? ???????????? ?????????????? false
        if (post == null) {
            return new PostSave();
        }

        ModerationStatus modStatus = ModerationStatus.NEW;
        switch (modStatusInstallRequest.getDecision()) {
            case "accept":
                modStatus = ModerationStatus.ACCEPTED;
                break;
            case "decline":
                modStatus = ModerationStatus.DECLINED;
                break;
        }

        post.setModerationStatus(modStatus);
        post.setModerator(moderatorUser);

        postRepository.save(post);
        PostSave postSave = new PostSave();
        postSave.setResult(true);

        return postSave;

    }

    public PostsByDate getPostsByDateForCalendar(Integer year) {

        //???????????????? ?????? ????????, ?????????? ???????? ????????????????????
        List<Integer> listYears = postRepository.getListYearsWhenPublishedPosts(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        //?? ????????????????, ???????? ???????????????? year ????????????????, ?????????????? ???????????? ?????????? ?????????? ???????? ?????????? ?????????????? ?????? ??????????
        List<DateWithQuantityPosts> queryDateAndQuantity;
        if (year == null) {
            queryDateAndQuantity = postRepository.getListDateWithQuantityPosts(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        } else {
            queryDateAndQuantity = postRepository.getListDateWithQuantityPostsByDate(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), year);
        }
        //listDateAndQuantity ?????? ???????????????????? ???????? ?? ???????????????????? ???????????? ?? ???????? ????????
        TreeMap<String, Long> listDateAndQuantity = new TreeMap<>();

        //?????????????? ?????????????????? ?????????????? ?? ?????????? ?? ?????????????????? listDateAndQuantity

        for (DateWithQuantityPosts obj : queryDateAndQuantity) {
            listDateAndQuantity.put(
                    new SimpleDateFormat("yyyy-MM-dd").format(obj.getDate()), obj.getQuantity());
        }

        PostsByDate postsByDate = new PostsByDate();
        postsByDate.setYears(listYears);
        postsByDate.setPosts(listDateAndQuantity);
        return postsByDate;

    }

    public Profile changeProfile(ProfileRequest profileRequest, Principal principal, MultipartFile photo,
                            HttpServletRequest request) throws IOException {

        boolean dataIsCorrect;
        Profile profile = new Profile();

        //???????????????? ???????????????? ????????????????????????
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user " + email + " not found"));

        dataIsCorrect = checkCorrectNewData(user, photo, profileRequest);
        //???????? ???????????? ?????????????? ???? ??????????????????, ?????????????? ????????????
        if (dataIsCorrect == false) {
            profile.setErrors(profile.new Error());
            return profile;
        }

        String pathFilePhoto = savePhotoToDisk(photo, request);
        //?????????????????? ?????????? ???????????? ????????????????????????
        user.setEmail(profileRequest.getEmail());
        user.setName(profileRequest.getName());
        if (profileRequest.getPassword() != null) {
            String encodePassword = passwordEncoder.encode(profileRequest.getPassword());
            user.setPassword(encodePassword);
        }
        if (profileRequest.getRemovePhoto() == 1) {
            user.setPhoto(null);
        } else {
            user.setPhoto(pathFilePhoto);
        }
        userRepository.save(user);
        profile.setResult(true);
        return profile;
    }

    private boolean checkCorrectNewData(User user, MultipartFile photo, ProfileRequest profileRequest) {
        long maxSizePhoto = 8388608;
        //?????????????????? ???????????????????? ???? ???????????????????????? ?? ?????????? ??????????????
        User userIsExist = userRepository.findByEmail(profileRequest.getEmail()).orElse(null);

        //???????? ?????????? ?????????????????????????????? ??????????, ?????? ???? ???????????????????? ?????? ?????????????? ????????????
        if (userIsExist == null || !userIsExist.equals(user) || profileRequest.getName().equals("")) {
            return false;
        }
        //???????? ???????????? 5???? ?????????????? ????????????
        if (photo != null) {
            if (photo.getSize() > maxSizePhoto) {
                return false;
            }
        }
        //???????????? ???????????? 6 ????????????????, ?????????????? ????????????
        String password = profileRequest.getPassword();
        if (password != null) {
            //???????? ???????????? ???????????? 6, ?????????????? ????????????
            if (password.length() < 6) {
                return false;
            }

        }
        //???????? ?????? ???????????? ??????????????????, ?????????????? true
        return true;
    }

    private String savePhotoToDisk(MultipartFile photo, HttpServletRequest request) throws IOException {
        if (photo != null) {
            String randomString = GeneralMethods.toGenerateRandomString(32);

            String originalFilename = photo.getOriginalFilename();
            String originalExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);

            String uploadDir = "/upload/avatars/";
            String newFilename = randomString + "." + originalExtension;


            String realPath = request.getServletContext().getRealPath(uploadDir);
            Path path = Path.of(realPath);
            if (!Files.exists(path)) {
                Files.createDirectories(Path.of(realPath));
            }
            File transferFile = new File(realPath + "/" + newFilename);

            BufferedImage imBuff = ImageIO.read(photo.getInputStream());

            imBuff = Scalr.resize(imBuff, 36, 36);

            ImageIO.write(imBuff, originalExtension, transferFile);

            return uploadDir + newFilename;
        }
        return "";
    }

    public Statistics getStatisticsMy(Principal principal) {
        Statistics statistics = new Statistics();
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        //???????????????? ???????????? ???????????? ???????????????????????? ?? ?????????? ?????????????????? ???????????????????? getTotalElements()
        Long quantityPosts = postRepository.getListPostsByUser(Short.parseShort("1"), user,
                ModerationStatus.ACCEPTED, GeneralMethods.getCurrentTimeUTC(), null)
                .getTotalElements();
        statistics.setPostsCount(quantityPosts);
        //??????????
        Long likesCount = postRepository.findAllLikeDislikeByUser(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), user, Short.parseShort("1"));
        statistics.setLikesCount(likesCount == null ? 0 : likesCount);
        //????????????????
        Long dislikesCount = postRepository.findAllLikeDislikeByUser(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), user, Short.parseShort("-1"));
        statistics.setDislikesCount(dislikesCount == null ? 0 : dislikesCount);
        //??????????????????
        Long viewsCount = postRepository.getViewCountPostsUser(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), user);
        statistics.setViewsCount(viewsCount == null ? 0 : viewsCount);
        //???????? ???????????? ????????????????????
        Timestamp firstPublication = postRepository.getFirstPostPublicationByUser(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), user);
        statistics.setFirstPublication(firstPublication == null ? 0 : firstPublication.getTime() / 1000);

        return statistics;
    }

    public ResponseEntity<Statistics> getStatisticsAll(Principal principal) {
        Statistics statistics = new Statistics();

        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        //???????? ???? ?????????????????? ?????????????? 401
        if (!user.getIsModerator() && STATISTICS_IS_PUBLIC == false) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        //???????????????? ???????????? ???????????? ???????? ?? ?????????? ?????????????????? ???????????????????? getTotalElements()
        long quantityPosts = postRepository.getListRecentPost(ModerationStatus.ACCEPTED, Short.parseShort("1"),
                GeneralMethods.getCurrentTimeUTC(), null)
                .getTotalElements();
        statistics.setPostsCount(quantityPosts);
        //??????????
        Long likesCount = postRepository.findAllLikeDislikeAll(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), Short.parseShort("1"));
        statistics.setLikesCount(likesCount == null ? 0 : likesCount);
        //????????????????
        Long dislikesCount = postRepository.findAllLikeDislikeAll(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), Short.parseShort("-1"));
        statistics.setDislikesCount(dislikesCount == null ? 0 : dislikesCount);
        //??????????????????
        Long viewsCount = postRepository.getViewCountPostsAll(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        statistics.setViewsCount(viewsCount == null ? 0 : viewsCount);
        //???????? ???????????? ????????????????????
        Timestamp firstPublication = postRepository.getFirstPostPublicationAll(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        statistics.setFirstPublication(firstPublication == null ? 0 : firstPublication.getTime() / 1000);

        return new ResponseEntity<>(statistics, HttpStatus.OK);

    }

    public void saveGlobalVariables(GlobalVariablesRequest globalVariablesRequest) {

        List<GlobalVariable> gList = globalVariablesRepository.findAll();
        //???????????????? ???????????????? ?? ???????????????????? ????????????????????
        MULTIUSER_MODE = globalVariablesRequest.isMultiuserMode();
        POST_PREMODERATION = globalVariablesRequest.isPostPremoderation();
        STATISTICS_IS_PUBLIC = globalVariablesRequest.isStatisticsIsPublic();

        //global ?????????????????? ???????????????????? ???????? ?????????? ???????????????????????? ?????????????????? ???? ???????? g.getCode() ????????????????
        Optional<GlobalVariable> global;
        //???????? ???? ???????? code ?? ???????????????? ???????????? ??????????????????, ???????????? ???????????? ???? ???????????? ????????
        global = gList.stream().filter(g -> g.getCode().equals("MULTIUSER_MODE")).findFirst();
        //?????????????????? ?????????????? ???? ???????????????? ???? ???????? ?? ?????????????????????? ???????????????? MULTIUSER_MODE
        global.ifPresent(g -> g.setValue(MULTIUSER_MODE ? "YES" : "NO"));

        //???????????????????? POST_PREMODERATION ?? STATISTICS_IS_PUBLIC ???????????????????? ???????????????? ????????

        global = gList.stream().filter(g -> g.getCode().equals("POST_PREMODERATION")).findFirst();
        global.ifPresent(g -> g.setValue(POST_PREMODERATION ? "YES" : "NO"));

        global = gList.stream().filter(g -> g.getCode().equals("STATISTICS_IS_PUBLIC")).findFirst();
        global.ifPresent(g -> g.setValue(STATISTICS_IS_PUBLIC ? "YES" : "NO"));

        globalVariablesRepository.saveAll(gList);

    }
}













