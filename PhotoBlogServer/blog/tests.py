from django.test import TestCase
from django.contrib.auth.models import User
from .models import Post

class PostApiTest(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(username='testuser', password='testpassword')
        self.post = Post.objects.create(
            author=self.user,
            title='자동 테스트 제목',
            text='자동 테스트 내용입니다.'
        )

    def test_post_api_get(self):
        response = self.client.get('/api_root/Post/')
        
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, '자동 테스트 제목')