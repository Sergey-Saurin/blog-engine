package main.controller;

import main.api.request.PostRequest;
import main.api.request.VoteRequest;
import main.dto.Post;
import main.dto.PostSave;
import main.dto.Posts;
import main.dto.Voice;
import main.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Date;

@RestController
@RequestMapping("/api/post")
public class ApiPostController {
    /////////////////////////////////////////////////////////////
    /**
     * GET
     */
    @Autowired
    private PostService postService;

    @GetMapping()
    public ResponseEntity<Posts> getPost(@RequestParam(value = "offset") int offset,
                                         @RequestParam(value = "limit") int limit,
                                         @RequestParam(value = "mode") String mode) {
        Posts posts = postService.getPosts(offset, limit, mode);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<Posts> getPostSearch(@RequestParam(value = "offset") int offset,
                                               @RequestParam(value = "limit") int limit,
                                               @RequestParam(value = "query", defaultValue = "") String query) {
        Posts posts = postService.searchPosts(offset, limit, query);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @GetMapping("/byDate")
    public ResponseEntity<Posts> getPostByDate(@RequestParam(value = "offset") int offset,
                                               @RequestParam(value = "limit") int limit,
                                               @RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date) {
        Posts posts = postService.getPostsByDate(offset, limit, date);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @GetMapping("/byTag")
    public ResponseEntity<Posts> getPostByTag(@RequestParam(value = "offset") int offset,
                                              @RequestParam(value = "limit") int limit,
                                              @RequestParam(value = "tag") String tagName) {

        Posts posts = postService.getPostsByTag(offset, limit, tagName);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @GetMapping("/moderation")
    public ResponseEntity<Posts> getPostModeration(@RequestParam(value = "offset") int offset,
                                                   @RequestParam(value = "limit") int limit,
                                                   @RequestParam(value = "status") String modStatusName,
                                                   Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Posts posts = postService.getMyModeration(offset, limit, modStatusName, principal);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @GetMapping("/my")
    public ResponseEntity<Posts> getPostMy(@RequestParam(value = "offset") int offset,
                                           @RequestParam(value = "limit") int limit,
                                           @RequestParam(value = "status") String status,
                                           Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Posts posts = postService.getMyPost(offset, limit, status, principal);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    @GetMapping("/{ID}")
    public ResponseEntity<Post> getPostMyId(@PathVariable("ID") int id,
                                            Principal principal) {
        Post post = postService.getPostById(id, principal);
        if (post == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(post, HttpStatus.OK);
    }


    /////////////////////////////////////////////////////////////

    /**
     * POST
     */

    @PostMapping()
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<PostSave> postPost(@RequestBody PostRequest postRequest,
                                             Principal principal) {
        PostSave postSave = postService.saveNewPost(postRequest, principal);
        return new ResponseEntity<>(postSave, HttpStatus.OK);
    }

    @PutMapping("/{ID}")
    public ResponseEntity<PostSave> postPostId(@PathVariable("ID") int id,
                                               @RequestBody PostRequest postRequest,
                                               Principal principal) {
        if (principal == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        PostSave postSave = postService.updatePost(id, postRequest, principal);
        return new ResponseEntity<>(postSave, HttpStatus.OK);
    }

    @PostMapping("/like")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<Voice> postLike(@RequestBody VoteRequest voteRequest, Principal principal) {
        Voice vote = postService.recordVoice(voteRequest, principal, Short.parseShort("1"));
        return new ResponseEntity<>(vote, HttpStatus.OK);
    }

    @PostMapping("/dislike")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<Voice> postDislike(@RequestBody VoteRequest voteRequest, Principal principal) {
        Voice vote = postService.recordVoice(voteRequest, principal, Short.parseShort("-1"));
        return new ResponseEntity<>(vote, HttpStatus.OK);
    }

}














