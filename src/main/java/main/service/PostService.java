package main.service;

import main.GeneralMethods;
import main.api.request.PostRequest;
import main.api.request.VoteRequest;
import main.dto.PostSave;
import main.dto.Posts;
import main.dto.Voice;
import main.enums.ModerationStatus;
import main.model.*;
import main.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostVoteRepository postVoteRepository;
    private final PostCommentRepository postCommentRepository;
    private final UserRepository userRepository;
    private final Tag2PostRepository tag2PostRepository;
    private final TagRepository tagRepository;

    public PostService(PostRepository postRepository,
                       PostVoteRepository postVoteRepository,
                       PostCommentRepository postCommentRepository,
                       UserRepository userRepository,
                       Tag2PostRepository tag2PostRepository,
                       TagRepository tagRepository) {
        this.postRepository = postRepository;
        this.postVoteRepository = postVoteRepository;
        this.postCommentRepository = postCommentRepository;
        this.userRepository = userRepository;
        this.tag2PostRepository = tag2PostRepository;
        this.tagRepository = tagRepository;
    }

    public Posts getPosts(int offset, int limit, String mode) {
        Posts posts = new Posts();

        Page<Post> postsPage;
        Pageable pageable = PageRequest.of(offset / limit, limit);
        switch (mode) {
            case "recent":
                postsPage = postRepository.getListRecentPost(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), pageable);
                break;
            case "popular":
                postsPage = postRepository.getListPopularPost(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), pageable);
                break;
            case "best":
                postsPage = postRepository.getListBestPost(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), pageable);
                break;
            case "early":
                postsPage = postRepository.getListEarlyPost(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), pageable);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mode);
        }
        //Количество постов одобренных модератором и активных
        posts.setCount(postsPage.getTotalElements());

        //Метод заполняет массив постов для dto объекта
        posts.setPosts(fillInArrayPostsForDtoPosts(posts, postsPage.getContent()));
        return posts;
    }

    public Posts getMyPost(int offset, int limit, String status, Principal principal) {
        String email = principal.getName();
        User userModel = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user " + email + " not found"));
        Posts posts = new Posts();

        Page<Post> postsPage;
        Pageable pageable = PageRequest.of(offset / limit, limit);
        switch (status) {
            case "inactive":
                postsPage = postRepository.getListInactivePostsByUser(Short.parseShort("0"), userModel, GeneralMethods.getCurrentTimeUTC(), pageable);
                break;
            case "pending":
                postsPage = postRepository.getListPostsByUser(Short.parseShort("1"), userModel, ModerationStatus.NEW, GeneralMethods.getCurrentTimeUTC(), pageable);
                break;
            case "declined":
                postsPage = postRepository.getListPostsByUser(Short.parseShort("1"), userModel, ModerationStatus.DECLINED, GeneralMethods.getCurrentTimeUTC(), pageable);
                break;
            case "published":
                postsPage = postRepository.getListPostsByUser(Short.parseShort("1"), userModel, ModerationStatus.ACCEPTED, GeneralMethods.getCurrentTimeUTC(), pageable);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + status);
        }

        posts.setCount(postsPage.getTotalElements());
        //Метод заполняет массив постов для dto объекта
        posts.setPosts(fillInArrayPostsForDtoPosts(posts, postsPage.getContent()));
        return posts;

    }

    public Posts searchPosts(int offset, int limit, String query) {
        Posts posts = new Posts();
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Post> postsPage = postRepository.getListPostsBySearchQuery(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), pageable, query);
        //Количество постов одобренных модератором и активных
        posts.setCount(postsPage.getTotalElements());

        //Метод заполняет массив постов для dto объекта
        posts.setPosts(fillInArrayPostsForDtoPosts(posts, postsPage.getContent()));

        return posts;

    }


    public Posts getPostsByDate(int offset, int limit, Date date) {
        Posts posts = new Posts();
        Pageable pageable = PageRequest.of(offset / limit, limit);

        Date startDay = GeneralMethods.getStartDayTimeUTC(date);
        Date endDay = GeneralMethods.getEndDayTimeUTC(date);

        Page<Post> postsPage = postRepository.getListPostsByDate(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), pageable, startDay, endDay);

        posts.setCount(postsPage.getTotalElements());

        posts.setPosts(fillInArrayPostsForDtoPosts(posts, postsPage.getContent()));

        return posts;
    }

    public Posts getPostsByTag(int offset, int limit, String tagName) {
        Posts posts = new Posts();
        Pageable pageable = PageRequest.of(offset / limit, limit);

        Page<Post> postsPage = tag2PostRepository.getListPostsByTag(ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC(), pageable, tagName);

        posts.setCount(postsPage.getTotalElements());

        posts.setPosts(fillInArrayPostsForDtoPosts(posts, postsPage.getContent()));

        return posts;
    }

    public Posts getMyModeration(int offset, int limit, String modStatusName, Principal principal) {
        Posts posts = new Posts();
        Pageable pageable = PageRequest.of(offset / limit, limit);


        //Получим ENUM по именя modStatusName, если modStatus = null(ошибка на фронте, не правильный параметр) тогда вернется пустой список постов
        ModerationStatus modStatus = GeneralMethods.getModerationStatusByName(modStatusName);
        //Список постов по статусу
        Page<Post> postsPage;
        /**В документации не написано как получать посты которые не были на модерации, при создании поста moderator_id = null
         * но если пост повторно отправлен на модерацию, пользователь внес изменения, тогда moderator_id != null
         * поэтому сделал выводить все посты с ModerationStatus.NEW если НЕ ModerationStatus.NEW тогда ищем по модератору*/
        if (modStatus.equals(ModerationStatus.NEW)) {
            postsPage = postRepository.getListNewPostsForModeration(modStatus, Short.parseShort("1"), pageable);
        } else {
            //Получим пользователя model
            String email = principal.getName();
            User userModel = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user " + email + " not found"));
            postsPage = postRepository.getListPostsModeration(modStatus, Short.parseShort("1"), pageable, userModel);
        }

        //Количество постов
        posts.setCount(postsPage.getTotalElements());

        //Заполнить список постов в dto
        posts.setPosts(fillInArrayPostsForDtoPosts(posts, postsPage.getContent()));

        return posts;
    }

    public main.dto.Post getPostById(int id, Principal principal) {
        Post postModel = postRepository.findActivePostById(id, ModerationStatus.ACCEPTED, Short.parseShort("1"), GeneralMethods.getCurrentTimeUTC());
        if (postModel == null) {
            return null;
        }

        main.dto.Post postDto = createDtoPost(postModel);
        //Увеличить просмотры поста
        increaseTheNumberOfViewsPost(postModel, principal);

        return postDto;
    }

    //fill in DTO Posts.post
    private List<Posts.Post> fillInArrayPostsForDtoPosts(Posts posts, List<Post> postsList) {
        List<Posts.Post> arrayPosts = new ArrayList<>();

        for (Post postModel : postsList) {

            Posts.Post postsPost = posts.new Post();
            postsPost.setId(postModel.getId());
            postsPost.setTimestamp(postModel.getTime().getTime() / 1000);
            //User{
            Posts.Post.User userPost = postsPost.new User();
            userPost.setId(postModel.getUser().getId());
            userPost.setName(postModel.getUser().getName());
            postsPost.setUser(userPost);
            //User}
            postsPost.setTitle(postModel.getTitle());
            postsPost.setAnnounce(postModel.getText().replaceAll("\\<.*?\\>", ""));
            postsPost.setLikeCount(postVoteRepository.findAllLikeDislikeByPost(Short.parseShort("1"), postModel));
            postsPost.setDislikeCount(postVoteRepository.findAllLikeDislikeByPost(Short.parseShort("-1"), postModel));
            postsPost.setCommentCount(postCommentRepository.getQuantityAllByPost(postModel));
            postsPost.setViewCount(postModel.getViewCount());

            arrayPosts.add(postsPost);
        }
        return arrayPosts;
    }

    private List<main.dto.Post.Comment> fillInArrayCommentsForDtoPost(main.dto.Post postDto, Post postModel) {
        List<main.dto.Post.Comment> commentList = new ArrayList<>();
        List<PostComment> comments = postCommentRepository.findAllCommentsByPost(postModel);

        for (PostComment postCommentModel : comments) {
            main.dto.Post.Comment postCommentDto = postDto.new Comment();
            postCommentDto.setId(postCommentModel.getId());
            postCommentDto.setTimestamp(postCommentModel.getTime().getTime() / 1000);
            postCommentDto.setText(postCommentModel.getText());
            //User{
            main.dto.Post.Comment.User userComment = postCommentDto.new User();
            userComment.setId(postCommentModel.getUser().getId());
            userComment.setName(postCommentModel.getUser().getName());
            userComment.setPhoto(postCommentModel.getUser().getPhoto());
            postCommentDto.setUser(userComment);
            //User}

            commentList.add(postCommentDto);
        }
        return commentList;
    }

    private main.dto.Post createDtoPost(Post postModel) {
        main.dto.Post postDto = new main.dto.Post();
        postDto.setId(postModel.getId());
        postDto.setTimestamp(postModel.getTime().getTime() / 1000);
        postDto.setActive(postModel.getIsActive());
        //User{
        main.dto.Post.AuthorUser userPost = postDto.new AuthorUser();
        userPost.setId(postModel.getUser().getId());
        userPost.setName(postModel.getUser().getName());
        postDto.setUser(userPost);
        //User}
        postDto.setTitle(postModel.getTitle());
        postDto.setText(postModel.getText());
        postDto.setLikeCount(postVoteRepository.findAllLikeDislikeByPost(Short.parseShort("1"), postModel));
        postDto.setDislikeCount(postVoteRepository.findAllLikeDislikeByPost(Short.parseShort("-1"), postModel));
        postDto.setViewCount(postModel.getViewCount());
        postDto.setComments(fillInArrayCommentsForDtoPost(postDto, postModel));
        postDto.setTags(tag2PostRepository.findAllTagsNameByPost(postModel));
        return postDto;
    }

    private void increaseTheNumberOfViewsPost(Post postModel, Principal principal) {   //increaseBy переменная для увеличения просмотров поста
        int increaseBy = 1;
        //Проверить пост просматривает его автор или модератор, тогда количество просмотров не увеличивается, если другой пользователь
        //авторизованный или не авторизованный, количество просмотров увеличиваем на 1
        if (principal != null) {
            String email = principal.getName();
            User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user " + email + " not found"));
            increaseBy = postModel.getUser().equals(currentUser) || currentUser.getIsModerator() ? 0 : 1;
        }
        //если автор поста или модератор просматривает пост, увеличивать просмотры не нужно
        if (increaseBy != 0) {
            postModel.setViewCount(postModel.getViewCount() + increaseBy);
            postRepository.save(postModel);
        }
    }


    public PostSave saveNewPost(PostRequest postRequest, Principal principal) {
        PostSave postSave = new PostSave();
        //Пост не соответствует требованиям
        if (postRequest.getTitle().length() < 3 || postRequest.getText().length() < 50) {
            postSave.setErrors(postSave.new ErrorSavePost());
            return postSave;
        }

        //Создание объекта
        Post post = createObjectPost(postRequest, principal);

        postRepository.save(post);
        postSave.setResult(true);

        //Создание тэга если такого не существует. Создать связь тэга и поста
        createTagAndTag2Post(postRequest, post);

        return postSave;
    }

    private Post createObjectPost(PostRequest postRequest, Principal principal) {
        //Получим текущего пользователя (автора)
        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user " + email + " not found"));

        Post post = new Post();
        post.setIsActive(postRequest.getIsActive());
        post.setModerationStatus(ModerationStatus.NEW);
        post.setUser(currentUser);
        Date currentTime = GeneralMethods.getCurrentTimeUTC();
        post.setTime(postRequest.getTime().before(currentTime) ? currentTime : postRequest.getTime());
        post.setTitle(postRequest.getTitle());
        post.setText(postRequest.getText());

        return post;
    }

    private void createTagAndTag2Post(PostRequest postRequest, Post post) {
        for (String tagName : postRequest.getTags()) {
            Tag tag = tagRepository.findTagByName(tagName);
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
                tagRepository.save(tag);
            }
            Tag2Post t2p = new Tag2Post();
            t2p.setPost(post);
            t2p.setTag(tag);
            tag2PostRepository.save(t2p);
        }
    }

    public PostSave updatePost(int id, PostRequest postRequest, Principal principal) {
        PostSave postSave = new PostSave();
        //Пост не соответствует требованиям
        if (postRequest.getTitle().length() < 3 || postRequest.getText().length() < 50) {
            postSave.setErrors(postSave.new ErrorSavePost());
            return postSave;
        }

        //Создание объекта
        Post post = updateObjectPost(id, postRequest, principal);

        postRepository.save(post);
        postSave.setResult(true);

        return postSave;
    }

    private Post updateObjectPost(int id, PostRequest postRequest, Principal principal) {
        //Получим текущего пользователя
        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user " + email + " not found"));

        Post post = postRepository.findById(id).orElseThrow();
        post.setIsActive(postRequest.getIsActive());
        if (!currentUser.getIsModerator()) {
            post.setModerationStatus(ModerationStatus.NEW);
        }
        Date currentTime = GeneralMethods.getCurrentTimeUTC();
        post.setTime(postRequest.getTime().before(currentTime) ? currentTime : postRequest.getTime());
        post.setTitle(postRequest.getTitle());
        post.setText(postRequest.getText());

        return post;
    }

    public Voice recordVoice(VoteRequest voteRequest, Principal principal, short valueVoice) {
        //Параметр "short valueVoice" это значение лайк или дизлайк, отправляется из соответствующего
        //метода в контроллере
        //Текущий голос - лайк или дизлайк
        Voice voice = new Voice();
        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email).orElse(null);
        Post post = postRepository.findById(voteRequest.getPostId()).orElse(null);
        //если не найден пользователь или пост, этого скорее всего не будет, но на всякий случай проверить
        if (currentUser == null || post == null) {
            return voice;
        }
        //currentVoice текущий голос пользователя лайк или дизлайк для поста, зависит из какого метода в контроллере пришел запрос
        PostVote currentVoice = postVoteRepository.findVoiceUserToPost(currentUser, post, valueVoice);
        //oppositeVoice противоположный голос пользователя для поста
        PostVote oppositeVoice = postVoteRepository.findVoiceUserToPost(currentUser, post, (short) (valueVoice * -1));
        //Если есть текущий голос для поста, вернуть false
        if (currentVoice != null) {
            return voice;
            //Если есть противоположный голос для поста, заменить его на текущий
        } else if (oppositeVoice != null) {
            oppositeVoice.setValue(valueVoice);
            oppositeVoice.setTime(new Date());
            postVoteRepository.save(oppositeVoice);
            //Если пользователь не устанавливал голос в текущем посту, записать новый голос лайк или дизлайк
        } else {
            PostVote postVote = new PostVote();
            postVote.setPost(post);
            postVote.setValue(valueVoice);
            postVote.setTime(new Date());
            postVote.setUser(currentUser);
            postVoteRepository.save(postVote);
        }
        voice.setResult(true);
        return voice;
    }
}









