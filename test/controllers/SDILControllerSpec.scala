/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sdil.controllers

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._
import sdil.connectors.SoftDrinksIndustryLevyConnector
import sdil.controllers.controllerhelpers._

class SDILControllerSpec extends PlayMessagesSpec with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach {

  val mockSdilConnector: SoftDrinksIndustryLevyConnector = mock[SoftDrinksIndustryLevyConnector]
  val controller = new SDILController(messagesApi, mockSdilConnector) {
    override def authConnector = mockAuthConnector
  }

  override def beforeEach() {
    reset(mockSdilConnector, mockAuthConnector)
  }

  "SDILController" should {
    "return Status: 200 Message: Return result is: true for successful auth with utr" in {
      sdilAuthMock(userWithUtr)
      val request = FakeRequest("GET", "/auth-test")
      val result = controller.testAuth.apply(request)

      status(result) mustBe OK
      contentAsString(result) must include("Return result is: true")
    }

    "return Status: SEE_OTHER when user logged in without utr and redirect to Sign In page" in {
      sdilAuthMock(userNoUtr)
      val request = FakeRequest("GET", "/auth-test")
      val result = controller.testAuth.apply(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get must include("gg/sign-in")
    }

    "return Status: SEE_OTHER when user not logged in and redirect to Sign In page" in {
      sdilAuthMock(notLoggedIn)
      val request = FakeRequest("GET", "/auth-test")
      val result = controller.testAuth.apply(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get must include("gg/sign-in")
    }

    "return Status: 200 when user is logged in and loads package page" in {
      sdilAuthMock(userWithUtr)
      val request = FakeRequest("GET", "/package")
      val result = controller.showPackage().apply(request)

      status(result) mustBe OK
      contentAsString(result) must include(messagesApi("sdil.package.heading"))
    }

    "return Status: See Other for package form POST with isLiable & ownBrands and redirect to package own" in {
      val request = FakeRequest().withFormUrlEncodedBody(validPackageForm: _*)
      val response = controller.submitPackage().apply(request)

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SDILController.showPackageOwn().url
    }

    "return Status: See Other for package form POST with isLiable & Brands and redirect to package own" in {
      val request = FakeRequest().withFormUrlEncodedBody(validPackageFormCustomersOnly: _*)
      val response = controller.submitPackage().apply(request)

      status(response) mustBe SEE_OTHER
      redirectLocation(response).get mustBe routes.SDILController.showPackageCopack().url
    }

    "return Status: Bad Request for invalid liability form POST request and display field hint .." in {
      val request = FakeRequest().withFormUrlEncodedBody(invalidPackageForm: _*)
      val response = controller.submitPackage().apply(request)

      status(response) mustBe BAD_REQUEST
      contentType(response).get mustBe HTML
      contentAsString(response) must include(messagesApi("sdil.form.radiocheck.error.summary"))
    }

  }
}
