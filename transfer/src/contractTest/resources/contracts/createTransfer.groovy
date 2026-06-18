package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should create a transfer"
    request {
        method 'POST'
        url '/transfer'
        headers {
            contentType(applicationJson())
        }
        body(
                fromLogin: "sender_user",
                toLogin: "receiver_user",
                amount: 100.00,
                comment: "Test transfer"
        )
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body(
                id: 1,
                fromAccountLogin: "sender_user",
                toAccountLogin: "receiver_user",
                amount: 100.00,
                status: "COMPLETED"
        )
    }
}
