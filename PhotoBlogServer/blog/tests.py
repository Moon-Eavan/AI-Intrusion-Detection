import tempfile

from django.conf import settings
from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase, override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Post

# Django test runner는 settings.DATABASES와 별개로 전용 테스트 DB를 새로 만들고
# 테스트가 끝나면 삭제하므로, 여기서 만드는 데이터는 실제 db.sqlite3(수동/운영 데이터)와
# 완전히 분리되어 있다. 업로드 테스트도 실제 media/ 디렉터리를 건드리지 않도록
# MEDIA_ROOT를 임시 디렉터리로 override한다.
TEST_MEDIA_ROOT = tempfile.mkdtemp(prefix='photoblog_test_media_')

# 1x1 투명 PNG (Pillow 없이도 유효한 ImageField 업로드가 되도록 실제 PNG 바이트 사용).
# API 테스트와 웹 뷰 테스트 양쪽에서 공용으로 사용한다.
PNG_1X1_BYTES = (
    b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01'
    b'\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01'
    b'\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82'
)


def make_test_png(name='test_image.png'):
    return SimpleUploadedFile(name, PNG_1X1_BYTES, content_type='image/png')


@override_settings(MEDIA_ROOT=TEST_MEDIA_ROOT)
class PostApiTests(APITestCase):
    """GET/POST /api_root/Post/ 자동 검증 (새 최종 결과보고서 ID 6)."""

    POST_LIST_URL = '/api_root/Post/'

    REQUIRED_FIELDS = ['author', 'title', 'text', 'created_date', 'published_date', 'image']

    @classmethod
    def setUpTestData(cls):
        User = get_user_model()
        cls.author = User.objects.create_user(username='api_test_author', password='unused-pw')
        cls.post1 = Post.objects.create(
            author=cls.author,
            title='자동 테스트 게시물 1',
            text='자동 테스트용 본문 1',
        )
        cls.post2 = Post.objects.create(
            author=cls.author,
            title='자동 테스트 게시물 2',
            text='자동 테스트용 본문 2',
        )

    def test_get_post_list_returns_200(self):
        response = self.client.get(self.POST_LIST_URL)
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_get_post_list_returns_json_array(self):
        response = self.client.get(self.POST_LIST_URL)
        self.assertIsInstance(response.data, list)
        self.assertEqual(len(response.data), 2)

    def test_get_post_list_items_have_required_fields(self):
        response = self.client.get(self.POST_LIST_URL)
        for item in response.data:
            for field in self.REQUIRED_FIELDS:
                self.assertIn(field, item)

    def test_get_post_list_field_values_match_created_posts(self):
        response = self.client.get(self.POST_LIST_URL)
        titles = {item['title'] for item in response.data}
        self.assertIn(self.post1.title, titles)
        self.assertIn(self.post2.title, titles)

    def test_post_create_post_returns_201(self):
        payload = {
            'title': '새로 업로드된 게시물',
            'text': '새로 업로드된 본문',
        }
        response = self.client.post(self.POST_LIST_URL, payload)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_post_create_post_reflects_title_and_text(self):
        payload = {
            'title': '필드 반영 확인용 제목',
            'text': '필드 반영 확인용 본문',
        }
        response = self.client.post(self.POST_LIST_URL, payload)
        self.assertEqual(response.data['title'], payload['title'])
        self.assertEqual(response.data['text'], payload['text'])

    def test_post_create_post_persists_to_database(self):
        payload = {
            'title': 'DB 저장 확인용 제목',
            'text': 'DB 저장 확인용 본문',
        }
        self.client.post(self.POST_LIST_URL, payload)
        self.assertTrue(Post.objects.filter(title=payload['title']).exists())

    def test_post_create_post_reflects_uploaded_image(self):
        payload = {
            'title': '이미지 업로드 확인용 제목',
            'text': '이미지 업로드 확인용 본문',
            'image': make_test_png(),
        }
        response = self.client.post(self.POST_LIST_URL, payload, format='multipart')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertTrue(response.data['image'])

        created = Post.objects.get(title=payload['title'])
        self.assertTrue(bool(created.image))

    def test_post_create_post_ignores_client_supplied_author(self):
        # author는 read_only_fields이므로 클라이언트가 보낸 값과 무관하게
        # 서버가 _resolve_default_author()로 결정한 작성자가 저장되어야 한다.
        other_author = get_user_model().objects.create_user(username='someone_else')
        payload = {
            'title': 'author 무시 확인용 제목',
            'text': 'author 무시 확인용 본문',
            'author': other_author.pk,
        }
        response = self.client.post(self.POST_LIST_URL, payload)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        created = Post.objects.get(title=payload['title'])
        self.assertNotEqual(created.author_id, other_author.pk)

    def test_post_create_post_missing_title_returns_400(self):
        payload = {
            'text': '제목 없이 보낸 본문',
        }
        response = self.client.post(self.POST_LIST_URL, payload)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('title', response.data)

    def test_post_create_post_missing_text_returns_400(self):
        payload = {
            'title': '본문 없이 보낸 제목',
        }
        response = self.client.post(self.POST_LIST_URL, payload)
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('text', response.data)

    def test_get_single_post_detail_returns_200(self):
        response = self.client.get(f'{self.POST_LIST_URL}{self.post1.pk}/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['title'], self.post1.title)

    def test_get_nonexistent_post_detail_returns_404(self):
        response = self.client.get(f'{self.POST_LIST_URL}999999/')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_post_create_authenticated_user_sets_author_to_logged_in_user(self):
        # perform_create()의 "로그인된 요청" 분기(self.request.user.is_authenticated)를 검증한다.
        # Android Client는 항상 비로그인 상태로 호출하지만, 브라우저에서 세션 인증된 요청이
        # 들어오는 경우에는 _resolve_default_author() 대신 실제 로그인 사용자가 author가 되어야 한다.
        self.client.login(username='api_test_author', password='unused-pw')
        payload = {
            'title': '로그인 사용자 API 작성 확인용 제목',
            'text': '로그인 사용자 API 작성 확인용 본문',
        }
        response = self.client.post(self.POST_LIST_URL, payload)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        created = Post.objects.get(title=payload['title'])
        self.assertEqual(created.author_id, self.author.pk)

    def test_post_create_with_no_existing_users_auto_creates_default_author(self):
        # perform_create() -> _resolve_default_author()의 세 번째 분기(슈퍼유저도, 일반
        # 사용자도 전혀 없는 완전히 빈 DB)를 검증한다. 이 분기는 'mobile_uploader' 계정을
        # 자동 생성해 업로드가 실패하지 않도록 하므로, 실제로 그렇게 동작하는지 확인한다.
        get_user_model().objects.all().delete()
        payload = {
            'title': '사용자 없음 상태에서 작성',
            'text': '사용자 없음 상태에서 작성한 본문',
        }
        response = self.client.post(self.POST_LIST_URL, payload)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        created = Post.objects.get(title=payload['title'])
        self.assertEqual(created.author.username, 'mobile_uploader')


@override_settings(MEDIA_ROOT=TEST_MEDIA_ROOT)
class PostModelTests(TestCase):
    """Post 모델의 동작(단순 필드 선언이 아닌 실제 메서드)을 검증한다."""

    def test_publish_sets_published_date_and_persists(self):
        post = Post.objects.create(
            author=get_user_model().objects.create_user(username='model_test_author'),
            title='publish 테스트',
            text='publish 테스트 본문',
        )
        self.assertIsNone(post.published_date)

        post.publish()

        self.assertIsNotNone(post.published_date)
        post.refresh_from_db()
        self.assertIsNotNone(post.published_date)

    def test_str_returns_title(self):
        post = Post.objects.create(
            author=get_user_model().objects.create_user(username='model_test_author2'),
            title='문자열 표현 확인용 제목',
            text='본문',
        )
        self.assertEqual(str(post), '문자열 표현 확인용 제목')


@override_settings(MEDIA_ROOT=TEST_MEDIA_ROOT)
class PostWebViewTests(TestCase):
    """post_list/post_detail/post_new/post_edit 웹 화면 자동 검증 (회귀 방지용)."""

    USERNAME = 'web_test_user'
    PASSWORD = 'web-test-pw-12345'
    OTHER_USERNAME = 'web_test_other'

    @classmethod
    def setUpTestData(cls):
        User = get_user_model()
        cls.user = User.objects.create_user(username=cls.USERNAME, password=cls.PASSWORD)
        cls.other_user = User.objects.create_user(username=cls.OTHER_USERNAME, password=cls.PASSWORD)
        cls.post = Post.objects.create(author=cls.user, title='웹 테스트 게시물', text='웹 테스트 본문')

    def login(self):
        self.client.login(username=self.USERNAME, password=self.PASSWORD)

    def test_post_list_returns_200_and_contains_title(self):
        response = self.client.get(reverse('post_list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.post.title)

    def test_post_detail_returns_200_and_contains_title(self):
        response = self.client.get(reverse('post_detail', args=[self.post.pk]))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.post.title)

    def test_post_detail_nonexistent_returns_404(self):
        response = self.client.get(reverse('post_detail', args=[999999]))
        self.assertEqual(response.status_code, 404)

    def test_post_new_anonymous_redirects_to_login(self):
        response = self.client.get(reverse('post_new'))
        self.assertEqual(response.status_code, 302)
        self.assertTrue(response.url.startswith(settings.LOGIN_URL))

    def test_post_edit_anonymous_redirects_to_login(self):
        response = self.client.get(reverse('post_edit', args=[self.post.pk]))
        self.assertEqual(response.status_code, 302)
        self.assertTrue(response.url.startswith(settings.LOGIN_URL))

    def test_post_new_authenticated_get_returns_200(self):
        self.login()
        response = self.client.get(reverse('post_new'))
        self.assertEqual(response.status_code, 200)

    def test_post_new_authenticated_post_creates_post_and_redirects(self):
        self.login()
        response = self.client.post(reverse('post_new'), {
            'title': '웹에서 새로 작성',
            'text': '웹에서 새로 작성한 본문',
        })
        created = Post.objects.get(title='웹에서 새로 작성')
        self.assertRedirects(response, reverse('post_detail', args=[created.pk]))
        self.assertEqual(created.author, self.user)
        self.assertIsNotNone(created.published_date)

    def test_post_new_authenticated_post_with_image_sets_image_field(self):
        self.login()
        response = self.client.post(reverse('post_new'), {
            'title': '이미지 포함 웹 작성',
            'text': '이미지 포함 웹 본문',
            'image': make_test_png('web_upload.png'),
        })
        created = Post.objects.get(title='이미지 포함 웹 작성')
        self.assertRedirects(response, reverse('post_detail', args=[created.pk]))
        self.assertTrue(bool(created.image))

    def test_post_edit_authenticated_get_returns_200_with_prefilled_form(self):
        # post_edit의 GET 분기(기존 게시물 instance로 PostForm을 미리 채워 보여주는 경로)를 검증한다.
        self.login()
        response = self.client.get(reverse('post_edit', args=[self.post.pk]))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, self.post.title)

    def test_post_edit_authenticated_post_updates_post_and_reassigns_author(self):
        # post_edit은 수정 시 post.author를 '원작성자'가 아니라 '현재 로그인한 편집자'로
        # 다시 설정한다 (post_new과 동일한 패턴). 이 동작이 조용히 바뀌지 않도록 다른 사용자가
        # 작성한 게시물을 self.user로 로그인해 수정하면서 author가 self.user로 바뀌는지 확인한다.
        other_post = Post.objects.create(
            author=self.other_user, title='다른 사용자가 작성한 글', text='다른 사용자 본문')

        self.login()
        response = self.client.post(reverse('post_edit', args=[other_post.pk]), {
            'title': '수정된 제목',
            'text': '수정된 본문',
        })

        other_post.refresh_from_db()
        self.assertRedirects(response, reverse('post_detail', args=[other_post.pk]))
        self.assertEqual(other_post.title, '수정된 제목')
        self.assertEqual(other_post.text, '수정된 본문')
        self.assertEqual(other_post.author, self.user)

    def test_post_edit_nonexistent_returns_404_even_when_authenticated(self):
        self.login()
        response = self.client.get(reverse('post_edit', args=[999999]))
        self.assertEqual(response.status_code, 404)
