package main.repository;

import main.constructorforquery.TagsAndWeight;
import main.enums.ModerationStatus;
import main.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {

    /**Чтоб всё было более однообразно, здесь тоже сделал два метода в зависимости от параметра query как в PostRepository
     * В документации написано что нужно прислать все тэги если query ="", если тэг не связан с постом он тоже возвращается с weight = 0
    */
            //Получаем все имена тэгов связанныз с постами и нет, весь список
    @Query("SELECT NEW main.constructorforquery.TagsAndWeight(t.name, " +
            //quantityPostByTag получаем количество постов связанным с тэгом{
            "(SELECT COUNT(*) FROM Tag2Post t2p " +
            "WHERE t = t2p.tag " +
            "AND t2p.post.isActive = :isActive AND t2p.post.moderationStatus = :modStatus AND t2p.post.time < :currentTime) as quantityPostByTag, " +
            //}
            //quantityAllPost Получаем общее количество постов активных и одобренных модератором меньше текущей даты
            //не зависимо связан посто с тэгом или нет, в документации сказано общее количество постов
            // Поэтому количество постов берем именно с таблицы POst, а не Tag2Post, так как какой-то пост может быть не связан с тэгом{
            "(SELECT COUNT(*) FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time < :currentTime) as quantityAllPost) " +
            "FROM Tag t " +
            //}
            //Сортироква по количеству постов связанных с тэгом, далее в рассчетах переменной weight будет использовано значение
            //quantityPostByTag первой из списка так как самая большая назовем maxWeight, рассчет
            //maxWeight = quantityPostByTag / quantityAllPost
            //Нормированный вес будет рассчитан
            //weight = quantityPostByTag / quantityAllPost / maxWeight
            //Получиться weight тэга с самым большим количеством постов будет 1.0 остальные меньше пропорционально количеству
            "ORDER BY quantityPostByTag DESC")
    List<TagsAndWeight> getAllTagsAndWeight(ModerationStatus modStatus, short isActive, Date currentTime);

    //Здесь всё тоже самое что выше в getAllTagsAndWeight, только добавлено "WHERE t.name LIKE %:tagName%" если параметр query
    //содержит значения, поиск тэга будет сравнивать содержит ли имя тэга значение query
    @Query("SELECT NEW main.constructorforquery.TagsAndWeight(t.name, " +
            "(SELECT COUNT(*) FROM Tag2Post t2p " +
            "WHERE t = t2p.tag " +
            "AND t2p.post.isActive = :isActive AND t2p.post.moderationStatus = :modStatus AND t2p.post.time < :currentTime) as quantityPostByTag, " +
            "(SELECT COUNT(*) FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time < :currentTime) as quantityAllPost) " +
            "FROM Tag t " +
            "WHERE t.name LIKE %:tagName% " +
            "GROUP BY t.name " +
            "ORDER BY quantityPostByTag DESC")
    List<TagsAndWeight> getTagsAndWeightByQuery(ModerationStatus modStatus, short isActive, Date currentTime, String tagName);

    Tag findTagByName(String name);
}
