USE cinema_design_demo;
SET NAMES utf8mb4;

INSERT INTO movie (
    movie_id, title, synopsis, genre,
    duration_minutes, poster_url, listing_status
) VALUES (
    1, '城南之夜', '两位老友在城南重逢。', '剧情',
    100, NULL, 'LISTED'
);

SELECT movie_id, title, synopsis, genre,
       duration_minutes, poster_url, listing_status
FROM movie
WHERE movie_id = 1;
