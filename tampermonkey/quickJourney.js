// ==UserScript==
// @name         ITSA - Self Assessment Refund - Quick Journey
// @namespace    http://tampermonkey.net/
// @version      2024-08-13
// @description  allow fast track through journey pages - no more typing!
// @author       Myles Offord
// @match        *://*/self-assessment-refund*
// @match        *://*/request-a-self-assessment-refund/*
// @match        *://*/iv-stub/uplift*
// @match        *://*/auth-login-stub/gg-sign-in*
// @match        *://*/bas-gateway/loggedout*
// @match        *://*/bas-gateway/sign-in*
// @icon         https://www.google.com/s2/favicons?sz=64&domain=undefined.localhost
// @grant        none
// ==/UserScript==

(function() {
    'use strict';

    document.body.appendChild(setup());
})();

const CARD_NINO = "AB200111C";
const BACS_NINO = "AB200111D";

const MTDIDID = "123";

function goToStart() {
    if (window.location.href.match(RegExp('localhost'))) {
        window.location.href = "http://localhost:9171/self-assessment-refund/test-only";
    } else {
        window.location.href = "/self-assessment-refund/test-only";
    }
}

function setup() {
    let panel = document.createElement('div');
    panel.id = "primary-button-panel";
    panel.style.position = 'absolute';
    panel.style.top = '50px';
    panel.style.lineHeight = '200%';
    panel.padding = "1px";

    panel.appendChild(createQuickButton('quickSubmit', 'Quick Submit', () => continueJourney()));

    setupConditionalControls(panel);

    panel.appendChild(document.createElement('br'));
    panel.appendChild(createQuickButton('goToStart', 'Start', () => goToStart()));

    return panel;
}

function setupConditionalControls(panel) {
    if (currentPageIs('/self-assessment-refund/test-only/start-journey')) {
        overrideQuickSubmitText(panel, 'Quick Submit (CARD)');
        panel.appendChild(document.createElement('br'));
        panel.appendChild(createQuickButton('bacsQuickSubmit', 'Quick Submit (BACS)', () => {
            document.getElementById('lastPaymentMethod').value = 'BACS';

            document.getElementById('fullAmount').value = "123.45";

            document.getElementById('nino').value = BACS_NINO;

            clickContinue();
        }));
    }

    if (currentPageIs('/auth-login-stub/gg-sign-in')) {
        overrideQuickSubmitText(panel, 'Quick Submit (Indvidual @ 50)');

        panel.appendChild(document.createElement('br'));
        panel.appendChild(createQuickButton('agentQuickSubmit', 'Quick Submit (Individual @ 250)', () => {
            // Auth Setup - Individual w/ NINO @ 50 CL
            document.getElementById('affinityGroupSelect').value = "Individual";

            document.getElementById('confidenceLevel').value = "250";

            document.getElementById('nino').value = CARD_NINO;

            document.getElementById('enrolment[0].name').value = "";
            document.getElementById('input-0-0-name').value = "";
            document.getElementById('input-0-0-value').value = "";

            clickContinue();
        }));

        panel.appendChild(document.createElement('br'));
        panel.appendChild(createQuickButton('agentQuickSubmit', 'Quick Submit (Agent @ 50)', () => {
            // Auth Setup - Agent w/ NINO @ 50 CL
            const affinityGroupSelect = document.getElementById('affinityGroupSelect');
            affinityGroupSelect.value = "Agent";
            affinityGroupSelect.onchange();

            document.getElementById('confidenceLevel').value = "50";

            document.getElementById('nino').value = "";

            document.getElementById('enrolment[0].name').value = "HMRC-MTD-IT";
            document.getElementById('input-0-0-name').value = "MTDITID";
            document.getElementById('input-0-0-value').value = "123";

            document.getElementById('js-add-delegated-enrolment').onclick();

            document.getElementById('delegatedEnrolment[0].key').value = "HMRC-MTD-IT";
            document.getElementById('input-delegated-0-0-name').value = "MTDITID";
            document.getElementById('input-delegated-0-0-value').value = "123";
            document.getElementById('delegatedEnrolment[0].delegatedAuthRule').value = "mtd-it-auth";

            clickContinue();
        }));

        panel.appendChild(document.createElement('br'));
        panel.appendChild(createQuickButton('agentDelegatedQuickSubmit', 'Quick Sbmit (Agent Delegated @ 50)', () => {
            const affinityGroupSelect = document.getElementById('affinityGroupSelect');
            affinityGroupSelect.value = "Agent";
            affinityGroupSelect.onchange();

            document.getElementById('confidenceLevel').value = "50";

            document.getElementById('nino').value = "";

            document.getElementById('js-add-delegated-enrolment').onclick();

            document.getElementById('delegatedEnrolment[0].key').value = "HMRC-MTD-IT";
            document.getElementById('input-delegated-0-0-name').value = "MTDITID";
            document.getElementById('input-delegated-0-0-value').value = "123";
            document.getElementById('delegatedEnrolment[0].delegatedAuthRule').value = "mtd-it-auth";

            clickContinue();
        }));

        panel.appendChild(document.createElement('br'));
        panel.appendChild(createRedButton('indvidualNoNinoQuickSubmit', 'Quick Submit (Indvidual @ 50 w/ Wrong NINO)', () => {
            // Auth Setup - Individual w/ NINO @ 50 CL with wrong NINO set.
            document.getElementById('affinityGroupSelect').value = "Individual";

            document.getElementById('confidenceLevel').value = "50";

            document.getElementById('nino').value = "PB999999D";

            document.getElementById('enrolment[0].name').value = "";
            document.getElementById('input-0-0-name').value = "";
            document.getElementById('input-0-0-value').value = "";

            clickContinue();
        }));

        panel.appendChild(document.createElement('br'));
        panel.appendChild(createRedButton('bacsQuickSubmit', 'Quick Submit (Agent @ 50 w/ Wrong MTD-IT-ID)', () => {
            // Auth Setup - Agent w/ NINO @ 50 CL & bad enrolment value
            document.getElementById('affinityGroupSelect').value = "Agent";

            document.getElementById('confidenceLevel').value = "50";

            document.getElementById('nino').value = "";

            document.getElementById('enrolment[0].name').value = "HMRC-MTD-IT";
            document.getElementById('input-0-0-name').value = "MTDITID";
            document.getElementById('input-0-0-value').value = "BADMTDITID";

            clickContinue();
        }));
    }

    if (currentPageIs('/bas-gateway/loggedout') || currentPageIs('/bas-gateway/sign-in')) {
        removeAllButtons(panel);
    }

    if (currentPageIs('/request-a-self-assessment-refund/refund-amount')) {
        overrideQuickSubmitText(panel, 'Recommended Amount');

        panel.appendChild(document.createElement('br'));
        panel.appendChild(createQuickButton('fullAmount', 'Full Amount', () => {
            document.getElementById('choice-full').checked = true;
            clickContinue();
        }));

        panel.appendChild(document.createElement('br'));
        panel.appendChild(createQuickButton('otherAmount', 'A Different Amount (£12.50)', () => {
            document.getElementById('choice-different').checked = true;
            document.getElementById('different-amount').value = '12.50';
            clickContinue();
        }));
    }

    if (currentPageIs('/request-a-self-assessment-refund/type-of-bank-account')) {
        overrideQuickSubmitText(panel, 'Personal Account');

        panel.appendChild(document.createElement('br'));
        panel.appendChild(createQuickButton('businessAccount', 'Business Account', () => {
            document.getElementById('accountType').checked = true;
            clickContinue();
        }));
    }

    if (currentPageIs('/request-a-self-assessment-refund/bank-building-society-details')) {
        /*
         * | 207106 | 86473611 | Security Engima | OK               | Business |
         * | 207106 | 86473611 | Security Engine | nameMatches - NO | Business |
         */
        overrideQuickSubmitText(panel, 'Personal Account');

        panel.appendChild(document.createElement('br'));
        panel.appendChild(createQuickButton('businessAccount', 'Business Account', () => {
            document.getElementById('accountName').value = 'Security Engima';
            document.getElementById('sortCode').value = '207106';
            document.getElementById('accountNumber').value = '86473611';
            clickContinue();
        }));
    }

    if (currentPageIs('/self-assessment-refund/refund-request-confirmation/')) {
        removeAllButtons(panel);
    }
}

function removeAllButtons(panel) {
    let button = panel.getElementsByTagName('button').quickSubmit;
    button.remove();

    let br = Array.from(panel.getElementsByTagName('br'));
    br.forEach((elem) => { elem.remove(); });
}

function createQuickButton(id, buttonText, callback) {
    let button = document.createElement('button');
    button.id = id;
    button.style.marginBottom = "0.2rem";

    if (!!document.getElementById('global-header')) {
        button.classList.add('button-start', 'govuk-!-display-none-print');
    } else {
        button.classList.add('govuk-button','govuk-!-display-none-print');
    }

    button.innerHTML = buttonText;
    button.onclick = callback

    return button;
}

function createRedButton(id, buttonText, callback) {
    let button = document.createElement('button');
    button.id = id;
    button.style.backgroundColor = "#700400";
    button.style.marginBottom = "0.2rem";

    if (!!document.getElementById('global-header')) {
        button.classList.add('button-start', 'govuk-!-display-none-print');
    } else {
        button.classList.add('govuk-button','govuk-!-display-none-print');
    }

    button.innerHTML = buttonText;
    button.onclick = callback

    return button;
}

function overrideQuickSubmitText(panel, text) {
    modifyTextById(panel, 'quickSubmit', text);
}

function modifyTextById(panel, id, text) {
    let target = panel.getElementsByTagName('*')[id];
    target.innerHTML = text;
}

function currentPageIs(path) {
    return window.location.pathname.match(RegExp(path));
}

const clickContinue = () => {
    let continueButton = document.getElementById('submit');
    if(continueButton) {
        continueButton.click();
    }
    else {
        document.getElementsByClassName('govuk-button')[0].click();
    }
}

/* ########################  Stubs pages  ######################## */

const authLoginStub = () => {
    if (currentPageIs('/auth-login-stub/gg-sign-in')) {
        // Auth Setup - Individual w/ NINO @ 50 CL
        document.getElementById('affinityGroupSelect').value = "Individual";

        document.getElementById('confidenceLevel').value = "50";

        document.getElementById('nino').value = CARD_NINO;

        document.getElementById('enrolment[0].name').value = "";
        document.getElementById('input-0-0-name').value = "";
        document.getElementById('input-0-0-value').value = "";

        clickContinue();
    }
};

const identitiyVerficationStub = () => {
    if (currentPageIs('/iv-stub/uplift')) {
        document.getElementById('forNino').value = CARD_NINO;
        document.getElementById('submit-continue').click();
    }
};

/* ########################  self assessment refunds pages  ######################## */

const testOnlyStartPage = () => {
  if (currentPageIs('/self-assessment-refund/test-only/start-journey')) {
      document.getElementById('lastPaymentMethod').value = 'CARD';

      document.getElementById('fullAmount').value = "987.65";

      document.getElementById('nino').value = CARD_NINO;

      clickContinue();
  }
};

const refundAmountPage = () => {
    if (currentPageIs('/request-a-self-assessment-refund/refund-amount')) {
        document.getElementById('choice-suggested').checked = true;
        clickContinue();
    }
};

const weNeedYourClientsBankDetails = () => {
    if (currentPageIs('/request-a-self-assessment-refund/we-need-your-clients-bank-details')) {
        clickContinue();
    }
};

const weNeedToGetYourBankDetails = () => {
    if (currentPageIs('/request-a-self-assessment-refund/how-you-will-get-the-refund')) {
        clickContinue();
    }
};

const accountDetailsType = () => {
    if (currentPageIs('/request-a-self-assessment-refund/type-of-bank-account')) {
        document.getElementById('accountType-2').checked = true; // Select Personal Account types
        clickContinue();
    }
};

const checkYourDetails = () => {
    if (currentPageIs('/self-assessment-refund/check-your-details')) {
        clickContinue();
    }
};

const enterBankDetails = () => {
    if (currentPageIs('/request-a-self-assessment-refund/bank-building-society-details')) {
        /*
         * | 405125 | 54344677 | Casandra Wilkinson | OK                | Personal |
         * | 405125 | 54344677 | Casandra Winkson   | nameMatches - NO  | Personal |
         */

        document.getElementById('accountName').value = 'Casandra Wilkinson';
        document.getElementById('sortCode').value = '405125';
        document.getElementById('accountNumber').value = '54344677';
        clickContinue();
    }
};

const dummyReauth = () => {
    if (currentPageIs('/self-assessment-refund/test-only/reauthentication')) {
        document.getElementById('continue').click();
    }
};

function continueJourney() {
    authLoginStub();
    identitiyVerficationStub();

    testOnlyStartPage();
    refundAmountPage();
    weNeedYourClientsBankDetails();
    weNeedToGetYourBankDetails();
    accountDetailsType();
    enterBankDetails();
    checkYourDetails();

    dummyReauth();
}

