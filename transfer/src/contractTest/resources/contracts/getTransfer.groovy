package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should get transfer by id"
    request {
        method 'GET'
        url '/transfer/1'
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
