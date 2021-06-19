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
        //global временная переменная куда будут записываться найденные по коду g.getCode() сущности
        Optional<GlobalVariable> global;
        //Ищем по полю code и получаем первый результат, больше одного не должно быть, если есть нужно менять метод записи сущностей быть
        global = gList.stream().filter(g -> g.getCode().equals("MULTIUSER_MODE")).findFirst();
        //Проверяем найдена ли сущность по коду и присваиваем значение MULTIUSER_MODE, если не найдено тогда false
        global.ifPresent(g -> MULTIUSER_MODE = g.getValue().equals("YES"));

        //Переменные POST_PREMODERATION и STATISTICS_IS_PUBLIC аналогично описанию выше

        global = gList.stream().filter(g -> g.getCode().equals("POST_PREMODERATION")).findFirst();
        global.ifPresent(g -> POST_PREMODERATION = g.getValue().equals("YES"));

        global = gList.stream().filter(g -> g.getCode().equals("STATISTICS_IS_PUBLIC")).findFirst();
        global.ifPresent(g -> STATISTICS_IS_PUBLIC = g.getValue().equals("YES"));

        GlobalVariables globalVariables = new GlobalVariables(MULTIUSER_MODE, POST_PREMODERATION, STATISTICS_IS_PUBLIC);
        return globalVariables;
    }

    public Tags getTags(String query) {
        List<TagsAndWeight> listTags;
        //Получим список тэгов, разбил на два метода, чтоб не склеивать запросы
        if (query.equals("")) {
            listTags = tagRepository.getAllTagsAndWeight(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        } else {
            listTags = tagRepository.getTagsAndWeightByQuery(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), query);
        }

        //Если список пустой тогда вернем пустой
        if (listTags.size() == 0) {
            return new Tags();
        }
        Tags tags = new Tags();
        //weightTagsList массив тэгов, после заполнения массив запишем его в dto.tags
        ArrayList<Tags.weightTag> weightTagsList = new ArrayList<>();

        //Получим максимальный весь, описывал в репозитории
        //Сортровка по убыванию количество постов у тэга, поэтому берем первый элемент списка listTags
        //делим количество постов у тэга на общее количество постов
        //maxWeight = quantityPostByTag / quantityAllPost
        //{
        double maxWeight = listTags.get(0).getQuantityPostByTag() / listTags.get(0).getQuantityAllPost();
        //}

        //обходим все объекты массива
        for (TagsAndWeight obj : listTags) {
            //weightTag подкласс класса dto.Tags
            Tags.weightTag weightTag = tags.new weightTag();
            //Для каждого элемента массива, т.е. имено поста проводим рассчет, так же делим количество постов тэга с общим количеством постов
            //и на maxWeight, получается Нормированный вес, для первого элемента будет равен 1.0
            //так как будет quantityPostByTag / quantityAllPost = maxWeight
            //остальные элементы будут меньше 1.0
            //maxWeight = quantityPostByTag / quantityAllPost
            //
            double weight = obj.getQuantityPostByTag() / obj.getQuantityAllPost() / maxWeight;
            weightTag.setName(obj.getName());
            //записываем weight в формате 0.00 в таком формате нужно отправить на фронт
            weightTag.setWeight(String.format("%.2f", weight));

            weightTagsList.add(weightTag);
        }
        tags.setTags(weightTagsList);
        return tags;
    }

    public String uploadImage(MultipartFile image) {
        //allowedFormat проверим разрешен ли формат файла
        boolean allowedFormat = checkFormatImage(Objects.requireNonNull(image.getContentType()));
        if (!allowedFormat) {
            return null;
        }
        //Считаем данные картинки
        InputStream inputStream = null;
        try {
            inputStream = image.getInputStream();
        } catch (IOException e) {
            return null;
        }
        //Создать подпапки для хранения картинки
        String pathFile = createPathImage(image.getOriginalFilename(), "upload");

        //Сохраним файл картинки
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
        //Создать все подпапки и заполнить путь к файлу
        StringBuilder pathImage = new StringBuilder();

        //Создать корневую папку если её нет
        Path path = Paths.get(pathImage.append(nameRootFolder).toString());
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(Path.of(pathImage.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Цикл обходим три раза, для создания трех подпапок, если подпапка с таким именем уже есть,
        //новая создана НЕ будет, будет использована созданная ранее
        for (int i = 0; i < 3; i++) {
            try {
                //Получим имя подпапки по индексам
                String nameDirectory = GeneralMethods.toGenerateRandomString(20);
                //Создадим подпапку и сразу добавим в путь к картинке эту подпапку
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
        //Проверить свойства комментария, если не удовлетворяет условия, вернуть ошибку 400 если не найден parentComment или post
        // или JSON если короткий текст
        ResponseEntity<CommentError> checkComment = checkPropertyComment(commentRequest, post, parentComment);
        if (checkComment != null) {
            return checkComment;
        }

        //Получить пользователя который написал комментарий
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
        //Поиск родительского поста
        //Если commentRequest.getParentId() == 0 значит комментарий написан к самому посту, иначе комментарий написан к другому комментарию
        if (commentRequest.getParentId() != 0) {
            //Если комментарий не найден, вернем BAD_REQUEST
            if (parentComment == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }

        //Поиск поста
        //Если пост не найден, вернем BAD_REQUEST
        if (post == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //Если комментарий слишком короткий, вернуть ошибку JSON
        if (commentRequest.getText().length() < 5) {
            CommentError commentError = new CommentError();
            commentError.setErrors(commentError.new Error());
            return new ResponseEntity<>(commentError, HttpStatus.OK);
        }
        //Всё отлично, можно сохранить комментарий
        return null;
    }

    public PostSave installModerationStatusForPost(ModerationStatusInstallRequest modStatusInstallRequest, Principal principal) {
        //Получить пользователя модератора, проверка разрешения на модерацию PreAuthorize
        String email = principal.getName();
        User moderatorUser = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user " + email + " not found"));

        Post post = postRepository.findById(modStatusInstallRequest.getPostId()).orElse(null);
        //Если пост не найден вернуть false
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

        //Получить все годы, когда были публикации
        List<Integer> listYears = postRepository.getListYearsWhenPublishedPosts(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        //С условием, если параметр year заполнен, вернуть тольуо посты этого года иначе вернуть все посты
        List<DateWithQuantityPosts> queryDateAndQuantity;
        if (year == null) {
            queryDateAndQuantity = postRepository.getListDateWithQuantityPosts(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        } else {
            queryDateAndQuantity = postRepository.getListDateWithQuantityPostsByDate(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), year);
        }
        //listDateAndQuantity для заполнения годв и количество постов в этом году
        TreeMap<String, Long> listDateAndQuantity = new TreeMap<>();

        //Обходим результат запроса в цикле и заполняем listDateAndQuantity

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

        //Получить текущего пользователя
        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user " + email + " not found"));

        dataIsCorrect = checkCorrectNewData(user, photo, profileRequest);
        //Если данные введены не корректно, вернуть ошибку
        if (dataIsCorrect == false) {
            profile.setErrors(profile.new Error());
            return profile;
        }

        String pathFilePhoto = savePhotoToDisk(photo, request);
        //Заполнить новые данные пользователя
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
        //Проверить существует ли пользователь с таким емайлом
        User userIsExist = userRepository.findByEmail(profileRequest.getEmail()).orElse(null);

        //Если емаил зарегистрирован ранее, или не корректное имя вернуть ошибку
        if (userIsExist == null || !userIsExist.equals(user) || profileRequest.getName().equals("")) {
            return false;
        }
        //Фото больше 5Мб вернуть ошибку
        if (photo != null) {
            if (photo.getSize() > maxSizePhoto) {
                return false;
            }
        }
        //Пароль короче 6 символов, вернуть ошибку
        String password = profileRequest.getPassword();
        if (password != null) {
            //Если пароль меньше 6, вернуть ошибку
            if (password.length() < 6) {
                return false;
            }

        }
        //Если все данные корректны, вернуть true
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
        //Получить список постов пользователя и сразу посчитать количество getTotalElements()
        Long quantityPosts = postRepository.getListPostsByUser(Short.parseShort("1"), user,
                ModerationStatus.ACCEPTED, GeneralMethods.getCurrentTimeUTC(), null)
                .getTotalElements();
        statistics.setPostsCount(quantityPosts);
        //Лайки
        Long likesCount = postRepository.findAllLikeDislikeByUser(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), user, Short.parseShort("1"));
        statistics.setLikesCount(likesCount == null ? 0 : likesCount);
        //Дизлайки
        Long dislikesCount = postRepository.findAllLikeDislikeByUser(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), user, Short.parseShort("-1"));
        statistics.setDislikesCount(dislikesCount == null ? 0 : dislikesCount);
        //Просмотры
        Long viewsCount = postRepository.getViewCountPostsUser(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), user);
        statistics.setViewsCount(viewsCount == null ? 0 : viewsCount);
        //Дата первой публикации
        Timestamp firstPublication = postRepository.getFirstPostPublicationByUser(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), user);
        statistics.setFirstPublication(firstPublication == null ? 0 : firstPublication.getTime() / 1000);

        return statistics;
    }

    public ResponseEntity<Statistics> getStatisticsAll(Principal principal) {
        Statistics statistics = new Statistics();

        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        //Если не модератор вернуть 401
        if (!user.getIsModerator() && STATISTICS_IS_PUBLIC == false) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        //Получить список постов всех и сразу посчитать количество getTotalElements()
        long quantityPosts = postRepository.getListRecentPost(ModerationStatus.ACCEPTED, Short.parseShort("1"),
                GeneralMethods.getCurrentTimeUTC(), null)
                .getTotalElements();
        statistics.setPostsCount(quantityPosts);
        //Лайки
        Long likesCount = postRepository.findAllLikeDislikeAll(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), Short.parseShort("1"));
        statistics.setLikesCount(likesCount == null ? 0 : likesCount);
        //Дизлайки
        Long dislikesCount = postRepository.findAllLikeDislikeAll(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), Short.parseShort("-1"));
        statistics.setDislikesCount(dislikesCount == null ? 0 : dislikesCount);
        //Просмотры
        Long viewsCount = postRepository.getViewCountPostsAll(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        statistics.setViewsCount(viewsCount == null ? 0 : viewsCount);
        //Дата первой публикации
        Timestamp firstPublication = postRepository.getFirstPostPublicationAll(ModerationStatus.ACCEPTED,
                Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        statistics.setFirstPublication(firstPublication == null ? 0 : firstPublication.getTime() / 1000);

        return new ResponseEntity<>(statistics, HttpStatus.OK);

    }

    public void saveGlobalVariables(GlobalVariablesRequest globalVariablesRequest) {

        List<GlobalVariable> gList = globalVariablesRepository.findAll();
        //Изменить значения в глобальных переменных
        MULTIUSER_MODE = globalVariablesRequest.isMultiuserMode();
        POST_PREMODERATION = globalVariablesRequest.isPostPremoderation();
        STATISTICS_IS_PUBLIC = globalVariablesRequest.isStatisticsIsPublic();

        //global временная переменная куда будут записываться найденные по коду g.getCode() сущности
        Optional<GlobalVariable> global;
        //Ищем по полю code и получаем первый результат, больше одного не должно быть
        global = gList.stream().filter(g -> g.getCode().equals("MULTIUSER_MODE")).findFirst();
        //Проверяем найдена ли сущность по коду и присваиваем значение MULTIUSER_MODE
        global.ifPresent(g -> g.setValue(MULTIUSER_MODE ? "YES" : "NO"));

        //Переменные POST_PREMODERATION и STATISTICS_IS_PUBLIC аналогично описанию выше

        global = gList.stream().filter(g -> g.getCode().equals("POST_PREMODERATION")).findFirst();
        global.ifPresent(g -> g.setValue(POST_PREMODERATION ? "YES" : "NO"));

        global = gList.stream().filter(g -> g.getCode().equals("STATISTICS_IS_PUBLIC")).findFirst();
        global.ifPresent(g -> g.setValue(STATISTICS_IS_PUBLIC ? "YES" : "NO"));

        globalVariablesRepository.saveAll(gList);

    }
}













